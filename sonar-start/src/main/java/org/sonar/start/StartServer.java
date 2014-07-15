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
package org.sonar.start;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessWrapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StartServer {

  private final static Logger LOGGER = LoggerFactory.getLogger(StartServer.class);

  private final static String SONAR_HOME = "SONAR_HOME";

  private final Env env;
  private final String esPort;
  private final Map<String, String> properties;

  private ExecutorService executor;
  private ProcessWrapper elasticsearch;
  private ProcessWrapper sonarqube;

  public StartServer(Env env) throws IOException {
    this(env, new String[]{});
  }

  public StartServer(Env env, String... args) throws IOException {
    this.env = env;
    this.executor = Executors.newFixedThreadPool(2);
    this.esPort = Integer.toString(NetworkUtils.freePort());
    this.properties = new HashMap<String, String>();

    if (Arrays.binarySearch(args, "--debug") > -1) {
      properties.put("esDebug", "true");
    }
  }

  public void shutdown() {
    LOGGER.info("Shutting down sonar Node");
  }

  public void start() {

    // Start ES
    this.startES(NetworkUtils.freePort());

    while (!elasticsearch.isReady()) {
      LOGGER.info("Waiting for ES");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // Start SQ
    //this.startSQ(NetworkUtils.freePort());

//    // And monitor the activity
//    try {
//      monitor.join();
//    } catch (InterruptedException e) {
//      LOGGER.warn("Shutting down the node...");
//    }
    //   shutdown();
  }


  private void startSQ(int port) {
    sonarqube = new ProcessWrapper(
      "org.sonar.application.StartServer",
      ImmutableMap.of(
        "SONAR_HOME", env.rootDir().getAbsolutePath(),
        "test", "test"),
      "SQ", port,
      env.rootDir().getAbsolutePath() + "/lib/server/sonar-application-4.5-SNAPSHOT.jar");
  }

  private void startES(int port) {
    elasticsearch = new ProcessWrapper(
      "org.sonar.search.ElasticSearch",
      ImmutableMap.of(
        "SONAR_HOME", env.rootDir().getAbsolutePath(),
        "esDebug", properties.containsKey("esDebug") ? properties.get("esDebug") : "false",
        "esPort", esPort,
        "esHome", env.rootDir().getAbsolutePath()),
      "ES", port,
      env.rootDir().getAbsolutePath() + "/lib/search/sonar-search-4.5-SNAPSHOT.jar");
  }

  public static void main(String... args) throws InterruptedException, IOException, URISyntaxException {

    String home = System.getenv(SONAR_HOME);
    //String home = "/Volumes/data/sonar/sonarqube/sonar-start/target/sonarqube-4.5-SNAPSHOT";

    //Check if we have a SONAR_HOME
    if (StringUtils.isEmpty(home)) {
      home = new File(".").getAbsolutePath();
    }

    new StartServer(new Env(home), args).start();
  }
}