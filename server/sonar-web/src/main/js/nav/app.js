define([
  'nav/global-navbar-view',
  'nav/context-navbar-view',
  'nav/settings-navbar-view'
], function (GlobalNavbarView, ContextNavbarView, SettingsNavbarView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      model = window.navbarOptions;

  App.addInitializer(function () {
    this.navbarView = new GlobalNavbarView({
      app: App,
      el: $('.navbar-global'),
      model: model
    });
    this.navbarView.render();
  });

  if (model.has('contextBreadcrumbs')) {
    App.addInitializer(function () {
      this.contextNavbarView = new ContextNavbarView({
        app: App,
        el: $('.navbar-context'),
        model: model
      });
      this.contextNavbarView.render();
    });
  }

  if (model.get('space') === 'settings') {
    App.addInitializer(function () {
      this.settingsNavbarView = new SettingsNavbarView({
        app: App,
        el: $('.navbar-context'),
        model: model
      });
      this.settingsNavbarView.render();
    });
  }

  App.addInitializer(function () {
    var navHeight = $('.navbar-global').outerHeight() + $('.navbar-context').outerHeight();
    $('.page-wrapper').css('padding-top', navHeight).data('top-offset', navHeight);
  });

  App.addInitializer(function () {
    var that = this;
    $(window).on('keypress', function (e) {
      var tagName = e.target.tagName;
      if (tagName !== 'INPUT' && tagName !== 'SELECT' && tagName !== 'TEXTAREA') {
        var code = e.keyCode || e.which;
        if (code === 63) {
          that.navbarView.showShortcutsHelp();
        }
      }
    });
  });

  App.addInitializer(function () {
    var that = this;
    $(window).on('keypress', function (e) {
      var tagName = e.target.tagName;
      if (tagName !== 'INPUT' && tagName !== 'SELECT' && tagName !== 'TEXTAREA') {
        var code = e.keyCode || e.which;
        if (code === 63) {
          that.navbarView.showShortcutsHelp();
        }
      }
    });
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
