// Generated by CoffeeScript 1.4.0
(function() {
  var AdminApp;

  yepnope({
    load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/admin.css', 'lib/lima1/net.js'],
    complete: function() {
      return $(document).ready(function() {
        var app;
        app = new AdminApp;
        return app.start();
      });
    }
  });

  AdminApp = (function() {

    AdminApp.prototype.SAVED_TOKEN = 'token';

    AdminApp.prototype.RIGHT_APPS_ADMIN = 'ApplicationsAdmin';

    AdminApp.prototype.RIGHT_USERS_ADMIN = 'UsersAdmin';

    AdminApp.prototype.RIGHT_SETTINGS_ADMIN = 'SettingsAdmin';

    AdminApp.prototype.DATE_FORMAT = 'yy/m/d';

    AdminApp.prototype.DATE_TIME_FORMAT = 'yy/m/d H:MM';

    function AdminApp() {
      var indicator, jqnet, url,
        _this = this;
      $(document.body).css({
        display: 'inherit'
      });
      url = '';
      if (_.startsWith(window.location.origin, 'chrome-extension://')) {
        url = 'https://lima1-kvj.rhcloud.com';
      }
      jqnet = new jQueryTransport(url);
      indicator = $('#network_indicator');
      jqnet.on_start = function() {
        return indicator.show();
      };
      jqnet.on_finish = function() {
        return indicator.fadeOut();
      };
      this.oauth = new OAuthProvider({
        clientID: 'lima1admin',
        scope: 'web'
      }, jqnet);
      this.oauth.on_token_error = function() {
        return _this.login();
      };
    }

    AdminApp.prototype.start = function() {
      var token;
      token = typeof window !== "undefined" && window !== null ? window.localStorage[this.SAVED_TOKEN] : void 0;
      this.initUI();
      if (token) {
        this.oauth.token = token;
        return this.loadUserInfo();
      } else {
        return this.login();
      }
    };

    AdminApp.prototype.hasRight = function(right) {
      var arr, r, _i, _len, _ref, _ref1;
      arr = (_ref = (_ref1 = this.user) != null ? _ref1.rights : void 0) != null ? _ref : [];
      for (_i = 0, _len = arr.length; _i < _len; _i++) {
        r = arr[_i];
        if (r === right) {
          return true;
        }
      }
      return false;
    };

    AdminApp.prototype.initUI = function(info) {
      var _ref, _ref1,
        _this = this;
      if (info == null) {
        info = null;
      }
      this.user = info;
      $('#main-user-name').text((_ref = (_ref1 = this.user) != null ? _ref1.username : void 0) != null ? _ref : '');
      if (this.user) {
        $('#main-user-info').show().unbind('click').bind('click', function() {
          return _this.showUserInfo();
        });
        $('#main-data-info').show().unbind('click').bind('click', function() {
          return _this.showDataInfo();
        });
        $('#main-logout').show().unbind('click').bind('click', function() {
          return _this.logout();
        });
        this.showUserInfo();
      } else {
        $('#main-user-info').hide();
        $('#main-data-info').hide();
        $('#main-logout').hide();
        this.showPane('no', 'no');
        this.showBreadcrumbs([]);
      }
      if (this.hasRight(this.RIGHT_USERS_ADMIN)) {
        $('#main-users').show().unbind('click').bind('click', function() {
          return _this.showUsersInfo();
        });
      } else {
        $('#main-users').hide();
      }
      if (this.hasRight(this.RIGHT_APPS_ADMIN)) {
        $('#main-apps').show().unbind('click').bind('click', function() {
          return _this.showAppsInfo();
        });
      } else {
        $('#main-apps').hide();
      }
      if (this.hasRight(this.RIGHT_SETTINGS_ADMIN)) {
        return $('#main-settings').show().unbind('click').bind('click', function() {
          return _this.showSettings();
        });
      } else {
        return $('#main-settings').hide();
      }
    };

    AdminApp.prototype.showBreadcrumbs = function(path) {
      var a, bc, i, item, li, _i, _ref, _results,
        _this = this;
      bc = $('#breadcrumb');
      bc.empty();
      _results = [];
      for (i = _i = 0, _ref = path.length; 0 <= _ref ? _i < _ref : _i > _ref; i = 0 <= _ref ? ++_i : --_i) {
        item = path[i];
        li = $(document.createElement('li')).appendTo(bc);
        if (i < path.length - 1) {
          a = $(document.createElement('a')).attr({
            href: '#'
          }).appendTo(li);
          a.text(item.caption);
          (function(item, a) {
            return a.bind('click', function() {
              return item.handler();
            });
          })(item, a);
          _results.push($(document.createElement('span')).addClass('divider').text('/').appendTo(li));
        } else {
          li.addClass('active');
          _results.push(li.text(item.caption));
        }
      }
      return _results;
    };

    AdminApp.prototype.showPane = function(linkID, paneID) {
      if (linkID) {
        $('#main-navigation li').removeClass('active');
        $('#' + linkID).parent('li').addClass('active');
      }
      if (paneID) {
        $('#panes div').removeClass('show');
        return $('#' + paneID).addClass('show');
      }
    };

    AdminApp.prototype.showUsersInfo = function() {
      var tbody,
        _this = this;
      this.showBreadcrumbs([
        {
          caption: 'Users'
        }
      ]);
      this.showPane('main-users', 'pane-users');
      $('#users-refresh').unbind('click').bind('click', function() {
        return _this.showUsersInfo();
      });
      tbody = $('#users-table tbody');
      return this.oauth.rest('', '/rest/admin/users/list?', null, function(err, data) {
        var btn, r, rights, td, tr, user, _i, _j, _len, _len1, _ref, _ref1, _ref2, _ref3, _ref4, _results;
        if (err) {
          return _this.showError(err);
        }
        tbody.empty();
        _ref = data != null ? data.list : void 0;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          user = _ref[_i];
          tr = $(document.createElement('tr')).appendTo(tbody);
          $(document.createElement('td')).text((_ref1 = user.username) != null ? _ref1 : '').appendTo(tr);
          $(document.createElement('td')).text((_ref2 = user.name) != null ? _ref2 : '').appendTo(tr);
          rights = '';
          _ref4 = (_ref3 = user != null ? user.rights : void 0) != null ? _ref3 : [];
          for (_j = 0, _len1 = _ref4.length; _j < _len1; _j++) {
            r = _ref4[_j];
            rights += r.charAt(0) + ' ';
          }
          $(document.createElement('td')).text(rights != null ? rights : '').appendTo(tr);
          td = $(document.createElement('td')).appendTo(tr);
          btn = $(document.createElement('button')).addClass('btn').appendTo(td);
          btn.text('Edit');
          _results.push((function(btn, user) {
            return btn.bind('click', function() {
              return _this.editUser(user, true);
            });
          })(btn, user));
        }
        return _results;
      });
    };

    AdminApp.prototype.editUser = function(info, admin) {
      var dt, emailEdit, nameEdit, tbody, _ref, _ref1,
        _this = this;
      if (admin) {
        this.showPane('main-users', 'pane-user-info');
        this.showBreadcrumbs([
          {
            caption: 'Users',
            handler: function() {
              return _this.showUsersInfo();
            }
          }, {
            caption: info.username
          }
        ]);
      } else {
        this.showPane('main-user-info', 'pane-user-info');
        this.showBreadcrumbs([
          {
            caption: info.username
          }
        ]);
      }
      nameEdit = $('#user-info-name');
      nameEdit.val((_ref = info.name) != null ? _ref : '').focus();
      emailEdit = $('#user-info-email');
      emailEdit.val((_ref1 = info.email) != null ? _ref1 : '');
      $('#user-info-username').text(info.username);
      $('#user-info-created').text(new Date(info.created).format(this.DATE_FORMAT));
      $('#user-info-save').unbind('click').bind('click', function() {
        info.name = nameEdit.val();
        info.email = emailEdit.val();
        _this.oauth.rest('', '/rest/admin/users/info/update?', JSON.stringify(info), function(err, data) {
          if (err) {
            return _this.showError(err);
          }
          return _this.editUser(data, admin);
        });
        return false;
      });
      tbody = $('#tokens-table tbody');
      tbody.empty();
      dt = new Date().getTime();
      return this.oauth.rest('', "/rest/admin/tokens/list?id=" + info.id + "&", null, function(err, data) {
        var btn, item, td, tr, _i, _len, _ref2, _ref3, _ref4, _results;
        if (err) {
          return _this.showError(err);
        }
        _ref2 = data.list;
        _results = [];
        for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
          item = _ref2[_i];
          tr = $(document.createElement('tr')).appendTo(tbody);
          $(document.createElement('td')).text((_ref3 = item.app) != null ? _ref3 : '').appendTo(tr);
          $(document.createElement('td')).text((_ref4 = item.ip) != null ? _ref4 : '').appendTo(tr);
          $(document.createElement('td')).text(new Date(item.created).format(_this.DATE_TIME_FORMAT)).appendTo(tr);
          $(document.createElement('td')).text(new Date(item.accessed).format(_this.DATE_TIME_FORMAT)).appendTo(tr);
          td = $(document.createElement('td')).appendTo(tr);
          btn = $(document.createElement('button')).addClass('btn btn-danger').appendTo(td);
          btn.text('Remove');
          _results.push((function(btn, item) {
            return btn.bind('click', function() {
              return _this.showPrompt('Are you sure want to remove token?', function() {
                return _this.oauth.rest('', '/rest/admin/tokens/remove?', JSON.stringify({
                  token: item.token,
                  user_id: info != null ? info.id : void 0
                }), function(err, data) {
                  if (err) {
                    return _this.showError(err);
                  }
                  return _this.editUser(info, admin);
                });
              });
            });
          })(btn, item));
        }
        return _results;
      });
    };

    AdminApp.prototype.addApp = function() {
      var dialog, name,
        _this = this;
      dialog = $('#add-app-dialog');
      name = $('#add-app-name').val('');
      dialog.modal({
        backdrop: 'static',
        keyboard: false
      });
      name.focus();
      dialog.find('#add-app-cancel').unbind('click').bind('click', function(e) {
        return dialog.modal('hide');
      });
      return dialog.find('#add-app-save').unbind('click').bind('click', function(e) {
        var app;
        app = name.val().trim().toLowerCase();
        if (!app) {
          return _this.showError('Name is required');
        }
        return _this.oauth.rest('', '/rest/admin/apps/add?', {
          app: app
        }, function(err, data) {
          if (err) {
            return _this.showError(err);
          }
          _this.editApp(data);
          return dialog.modal('hide');
        });
      });
    };

    AdminApp.prototype.editApp = function(app) {
      var descEdit, nameEdit, schemaEdit, _ref, _ref1, _ref2, _ref3,
        _this = this;
      this.showPane(null, 'pane-app-info');
      this.showBreadcrumbs([
        {
          caption: 'Applications',
          handler: function() {
            return _this.showAppsInfo();
          }
        }, {
          caption: app.app
        }
      ]);
      $('#app-info-app').text(app.app);
      $('#app-info-rev').text((_ref = app.rev) != null ? _ref : '-');
      nameEdit = $('#app-info-name').val((_ref1 = app.name) != null ? _ref1 : '').focus();
      descEdit = $('#app-info-desc').val((_ref2 = app.desc) != null ? _ref2 : '');
      schemaEdit = $('#app-info-schema').val((_ref3 = app.schema) != null ? _ref3 : '{\n}');
      return $('#app-info-save').unbind('click').bind('click', function() {
        var schema;
        try {
          schema = JSON.parse(schemaEdit.val());
        } catch (e) {
          return _this.showError('Invalid JSON provided');
        }
        app.schema = schemaEdit.val();
        app.name = nameEdit.val();
        app.desc = descEdit.val();
        _this.oauth.rest('', '/rest/admin/apps/update?', app, function(err, data) {
          if (err) {
            return _this.showError(err);
          }
          return _this.editApp(app);
        }, {
          test: app
        });
        return false;
      });
    };

    AdminApp.prototype.showAppsInfo = function() {
      var tbody,
        _this = this;
      this.showPane('main-apps', 'pane-apps');
      this.showBreadcrumbs([
        {
          caption: 'Applications'
        }
      ]);
      $('#apps-create').unbind('click').bind('click', function() {
        return _this.addApp();
      });
      $('#apps-refresh').unbind('click').bind('click', function() {
        return _this.showAppsInfo();
      });
      tbody = $('#apps-table tbody');
      return this.oauth.rest('', '/rest/admin/apps/list?', null, function(err, data) {
        var btn, item, td, tr, _i, _len, _ref, _ref1, _ref2, _ref3, _results;
        if (err) {
          return _this.showError(err);
        }
        tbody.empty();
        _ref = data != null ? data.list : void 0;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          item = _ref[_i];
          tr = $(document.createElement('tr')).appendTo(tbody);
          $(document.createElement('td')).text((_ref1 = item.app) != null ? _ref1 : '').appendTo(tr);
          $(document.createElement('td')).text((_ref2 = item.name) != null ? _ref2 : '').appendTo(tr);
          $(document.createElement('td')).text((_ref3 = item.rev) != null ? _ref3 : '-').appendTo(tr);
          td = $(document.createElement('td')).appendTo(tr);
          btn = $(document.createElement('button')).addClass('btn').appendTo(td);
          btn.text('Edit');
          _results.push((function(btn, item) {
            return btn.bind('click', function() {
              return _this.editApp(item);
            });
          })(btn, item));
        }
        return _results;
      });
    };

    AdminApp.prototype.showDataInfo = function() {
      var tbody,
        _this = this;
      this.showPane('main-data-info', 'pane-data-info');
      this.showBreadcrumbs([
        {
          caption: 'Data info'
        }
      ]);
      $('#data-info-refresh').unbind('click').bind('click', function() {
        return _this.showDataInfo();
      });
      tbody = $('#data-info-table tbody');
      return this.oauth.rest('', '/rest/admin/data/list?', null, function(err, data) {
        var item, tr, _i, _len, _ref, _ref1, _ref2, _results;
        if (err) {
          return _this.showError(err);
        }
        tbody.empty();
        _ref = data != null ? data.list : void 0;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          item = _ref[_i];
          tr = $(document.createElement('tr')).appendTo(tbody);
          $(document.createElement('td')).text((_ref1 = item.app) != null ? _ref1 : '').appendTo(tr);
          $(document.createElement('td')).text((_ref2 = item.name) != null ? _ref2 : '').appendTo(tr);
          _results.push((function(item) {
            var btnBackupData, btnBackupFiles, btnRemove, btnRestore, btnStat, downloadBackup, files, loadStat, statTable, td;
            td = $(document.createElement('td')).appendTo(tr);
            btnStat = $(document.createElement('button')).addClass('btn btn-info').text('Load').appendTo(td);
            btnStat.bind('click', function() {
              return loadStat();
            });
            statTable = $(document.createElement('table')).addClass('table table-condensed stat-table').appendTo(td);
            downloadBackup = function(type) {
              var dt, file, url;
              dt = new Date().format('yyyymmdd_HHMM');
              file = "" + item.app + "_" + type + "_" + dt + ".zip";
              url = "/rest/backup?fname=" + file + "&type=" + type + "&";
              return window.open(_this.oauth.getFullURL(item.app, url));
            };
            loadStat = function() {
              return _this.oauth.rest(item.app, '/rest/admin/data/stat?', null, function(err, data) {
                var addRow, i, num, size, sizeText, sizes, st, sz, _j, _k, _len1, _ref3, _ref4, _ref5, _ref6;
                statTable.empty();
                addRow = function(caption, value, class_caption, class_value) {
                  tr = $(document.createElement('tr')).appendTo(statTable);
                  td = $(document.createElement('td')).addClass('stat-td-caption').appendTo(tr);
                  td.text(caption);
                  if (class_caption) {
                    td.addClass(class_caption);
                  }
                  td = $(document.createElement('td')).addClass('stat-td-value').appendTo(tr);
                  td.text(value);
                  if (class_value) {
                    return td.addClass(class_value);
                  }
                };
                addRow('Objects:', data.t, 'stat-td-bold', 'stat-td-bold');
                _ref4 = (_ref3 = data.d) != null ? _ref3 : [];
                for (_j = 0, _len1 = _ref4.length; _j < _len1; _j++) {
                  st = _ref4[_j];
                  addRow("" + st.s + ":", st.c);
                }
                addRow('Files:', data.f, 'stat-td-bold', 'stat-td-bold');
                size = (_ref5 = data.fs) != null ? _ref5 : 0;
                sizes = [
                  {
                    size: 1024,
                    suffix: 'B'
                  }, {
                    size: 1024 * 1024,
                    suffix: 'Kb'
                  }, {
                    size: 1024 * 1024 * 1024,
                    suffix: 'Mb'
                  }, {
                    size: 1024 * 1024 * 1024 * 1024,
                    suffix: 'Gb'
                  }
                ];
                sizeText = '???';
                for (i = _k = 0, _ref6 = sizes.length; 0 <= _ref6 ? _k < _ref6 : _k > _ref6; i = 0 <= _ref6 ? ++_k : --_k) {
                  sz = sizes[i];
                  if (size < sz.size || i === sizes.length - 1) {
                    num = Math.round(size * 10240 / sz.size, 0) / 10;
                    sizeText = "" + num + " " + sz.suffix;
                    break;
                  }
                }
                return addRow('Size:', sizeText);
              }, {
                test: {
                  t: 11,
                  f: 0,
                  fs: 1234567,
                  d: [
                    {
                      s: 'notes',
                      c: 99
                    }, {
                      s: 'sheets',
                      c: 5
                    }
                  ]
                }
              });
            };
            td = $(document.createElement('td')).appendTo(tr);
            btnBackupData = $(document.createElement('button')).addClass('btn').text('Data').appendTo(td);
            btnBackupData.bind('click', function() {
              return downloadBackup('data');
            });
            $(document.createElement('span')).text(' ').appendTo(td);
            btnBackupFiles = $(document.createElement('button')).addClass('btn').text('Files').appendTo(td);
            btnBackupFiles.bind('click', function() {
              return downloadBackup('file');
            });
            td = $(document.createElement('td')).appendTo(tr);
            files = $(document.createElement('input')).attr({
              type: 'file',
              multiple: 'multiple'
            }).addClass('data-restore-files').appendTo(td);
            files.bind('change', function(e) {
              var fileList, _ref3;
              fileList = (_ref3 = files.get(0)) != null ? _ref3.files : void 0;
              log('Changed:', fileList);
              if ((fileList != null ? fileList.length : void 0) > 0) {
                return _this.showPrompt('Are you sure want to restore from backup from ' + (fileList != null ? fileList.length : void 0) + ' files provided? It will replace all your data', function() {
                  return _this.showError('Not implemented');
                });
              }
            });
            btnRestore = $(document.createElement('button')).addClass('btn btn-warning').text('Restore').appendTo(td);
            btnRestore.bind('click', function() {
              return files.click();
            });
            td = $(document.createElement('td')).appendTo(tr);
            btnRemove = $(document.createElement('button')).addClass('btn btn-danger').text('Clear').appendTo(td);
            return btnRemove.bind('click', function() {
              return _this.showPrompt('Are you sure want to remove all associated data?', function() {
                return _this.oauth.rest(item.app, '/rest/admin/data/clear?', item, function(err, data) {
                  if (err) {
                    return _this.showError(err);
                  }
                  _this.showAlert("Data removed for application " + item.app);
                  return loadStat();
                }, {
                  test: item
                });
              });
            });
          })(item));
        }
        return _results;
      }, {
        test: {
          list: [
            {
              id: 0,
              app: 'whiskey2',
              rev: 1
            }, {
              id: 1,
              app: 'sstack',
              name: 'StickStack application'
            }
          ]
        }
      });
    };

    AdminApp.prototype.showUserInfo = function() {
      return this.editUser(this.user, false);
    };

    AdminApp.prototype.loadUserInfo = function() {
      var _this = this;
      return this.oauth.rest('', '/rest/admin/users/info?', null, function(err, data) {
        if (err) {
          return _this.showError(err);
        }
        log('UserInfo:', data);
        return _this.initUI(data);
      });
    };

    AdminApp.prototype.logout = function() {
      var _this = this;
      return this.showPrompt('Really logout?', function() {
        if (typeof window !== "undefined" && window !== null) {
          delete window.localStorage[_this.SAVED_TOKEN];
        }
        _this.initUI();
        return _this.login();
      });
    };

    AdminApp.prototype.login = function() {
      var dialog, password, remember, username,
        _this = this;
      dialog = $('#main-login-dialog');
      username = dialog.find('#login-username');
      password = dialog.find('#login-password');
      remember = dialog.find('#login-remember');
      password.val('');
      dialog.modal({
        backdrop: 'static',
        keyboard: false
      });
      username.focus();
      return dialog.find('#login-do-login').unbind('click').bind('click', function(e) {
        return _this.oauth.tokenByUsernamePassword(username.val(), password.val(), function(err, data) {
          var _ref;
          if (err) {
            _this.showError((_ref = err.error_description) != null ? _ref : err);
            password.val('');
            username.focus();
            return;
          }
          if (typeof window !== "undefined" && window !== null) {
            delete window.localStorage[_this.SAVED_TOKEN];
          }
          if (remember.attr('checked')) {
            if (typeof window !== "undefined" && window !== null) {
              window.localStorage[_this.SAVED_TOKEN] = data.access_token;
            }
          }
          log('Token', data, remember.attr('checked'));
          dialog.modal('hide');
          return _this.loadUserInfo();
        });
      });
    };

    AdminApp.prototype.showError = function(message) {
      return this.showAlert(message, {
        severity: 'error'
      }, 'Error');
    };

    AdminApp.prototype.showPrompt = function(message, handler) {
      var alert, button, div,
        _this = this;
      div = $(document.createElement('p'));
      button = $(document.createElement('button')).addClass('btn btn-danger').text('Proceed').appendTo(div);
      button.bind('click', function(e) {
        if (handler) {
          handler();
        }
        return alert.remove();
      });
      return alert = this.showAlert(message, {
        persistent: true,
        severity: 'block',
        content: div
      }, 'Prompt');
    };

    AdminApp.prototype.showAlert = function(message, config, title) {
      var div, _ref, _ref1,
        _this = this;
      if (title == null) {
        title = 'Lima1';
      }
      div = $(document.createElement('div')).appendTo($('#alerts')).addClass('alert alert-' + ((_ref = config != null ? config.severity : void 0) != null ? _ref : 'info'));
      $(document.createElement('button')).appendTo(div).addClass('close').attr({
        'data-dismiss': 'alert'
      }).html('&times;');
      $(document.createElement('h4')).appendTo(div).text(title != null ? title : 'Untitled');
      $(document.createElement('span')).appendTo(div).text(message);
      if (config != null ? config.content : void 0) {
        div.append(config.content);
      }
      if (!(config != null ? config.persistent : void 0)) {
        setTimeout(function() {
          return div.remove();
        }, (_ref1 = config != null ? config.timeout : void 0) != null ? _ref1 : 3000);
      }
      return div;
    };

    return AdminApp;

  })();

}).call(this);
