/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations.v51;

import org.apache.commons.dbutils.DbUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.source.db.FileSourceDb;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class FeedFileSourcesBinaryDataTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(FeedFileSourcesBinaryDataTest.class, "schema.sql");

  @Test
  public void convert_csv_to_protobuf() throws Exception {
    db.prepareDbUnit(getClass(), "data.xml");

    DatabaseMigration migration = new FeedFileSourcesBinaryData(db.database());
    migration.execute();

    int count = db.countSql("select count(*) from file_sources where binary_data is not null");
    assertThat(count).isEqualTo(3);

    try(Connection connection = db.openConnection()) {
      FileSourceDb.Data data = selectData(connection, 1L);
      assertThat(data.getLinesCount()).isEqualTo(4);
      assertThat(data.getLines(0).getScmRevision()).isEqualTo("aef12a");

      data = selectData(connection, 2L);
      assertThat(data.getLinesCount()).isEqualTo(4);
      assertThat(data.getLines(0).hasScmRevision()).isFalse();

      data = selectData(connection, 3L);
      assertThat(data.getLinesCount()).isEqualTo(0);
    }
  }

  @Test
  public void fail_to_parse_csv() throws Exception {
    db.prepareDbUnit(getClass(), "bad_data.xml");

    DatabaseMigration migration = new FeedFileSourcesBinaryData(db.database());
    try {
      migration.execute();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("Invalid FILE_SOURCES.DATA on row with ID 1:");
    }
  }

  private FileSourceDb.Data selectData(Connection connection, long fileSourceId) throws SQLException {
    PreparedStatement pstmt = connection.prepareStatement("select binary_data from file_sources where id=?");
    ResultSet rs = null;
    try {
      pstmt.setLong(1, fileSourceId);
      rs = pstmt.executeQuery();
      rs.next();
      InputStream data = rs.getBinaryStream(1);
      return FileSourceDto.decodeData(data);
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
    }
  }
}
