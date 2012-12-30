yepnope({
  load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/admin.css', 'lib/lima1/net.js'],
  complete: ->
    $(document).ready(->
      app = new AdminApp
      app.start()
    );
})

class AdminApp

  SAVED_TOKEN: 'token'
  RIGHT_APPS_ADMIN: 'ApplicationsAdmin'
  RIGHT_USERS_ADMIN: 'UsersAdmin'
  RIGHT_SETTINGS_ADMIN: 'SettingsAdmin'
  DATE_FORMAT: 'yy/m/d'
  DATE_TIME_FORMAT: 'yy/m/d H:MM'

  constructor: ->
    $(document.body).css(display: 'inherit')
    url = ''
    if _.startsWith(window.location.origin, 'chrome-extension://')
      url = 'https://lima1-kvj.rhcloud.com'
    jqnet = new jQueryTransport(url)
    indicator = $('#network_indicator')
    jqnet.on_start = =>
      indicator.show()
    jqnet.on_finish = =>
      indicator.fadeOut()
    @oauth = new OAuthProvider {
      clientID: 'lima1admin'
      scope: 'web'
    }, jqnet
    @oauth.on_token_error = =>
      @login()

  start: ->
    token = window?.localStorage[@SAVED_TOKEN]
    @initUI()
    if token
      @oauth.token = token
      @loadUserInfo()
    else
      @login()

  hasRight: (right) ->
    arr = @user?.rights ? []
    for r in arr
      if r is right then return yes
    return no

  initUI: (info = null) ->
    @user = info
    $('#main-user-name').text(@user?.username ? '')
    if @user
      $('#main-user-info').show().unbind('click').bind 'click', () =>
        @showUserInfo()
      $('#main-data-info').show().unbind('click').bind 'click', () =>
        @showDataInfo()
      $('#main-logout').show().unbind('click').bind 'click', () =>
        @logout()
      @showUserInfo()
    else
      $('#main-user-info').hide()
      $('#main-data-info').hide()
      $('#main-logout').hide()
      @showPane('no', 'no')
      @showBreadcrumbs([])
    if @hasRight @RIGHT_USERS_ADMIN
      $('#main-users').show().unbind('click').bind 'click', () =>
        @showUsersInfo()
    else
      $('#main-users').hide()
    if @hasRight @RIGHT_APPS_ADMIN
      $('#main-apps').show().unbind('click').bind 'click', () =>
        @showAppsInfo()
    else
      $('#main-apps').hide()
    if @hasRight @RIGHT_SETTINGS_ADMIN
      $('#main-settings').show().unbind('click').bind 'click', () =>
        @showSettings()
    else
      $('#main-settings').hide()

  showBreadcrumbs: (path) ->
    bc = $('#breadcrumb')
    bc.empty()
    for i in [0...path.length]
      item = path[i]
      li = $(document.createElement('li')).appendTo(bc)
      if i<path.length-1
        a = $(document.createElement('a')).attr(href: '#').appendTo(li)
        a.text(item.caption)
        do (item, a) =>
          a.bind 'click', () =>
            item.handler()
        $(document.createElement('span')).addClass('divider').text('/').appendTo(li)
      else
        li.addClass('active')
        li.text(item.caption)

  showPane: (linkID, paneID) ->
    if linkID
      $('#main-navigation li').removeClass('active')
      $('#'+linkID).parent('li').addClass('active')
    if paneID
      $('#panes div').removeClass('show')
      $('#'+paneID).addClass('show')

  showUsersInfo: ->
    @showBreadcrumbs [{caption: 'Users'}]
    @showPane('main-users', 'pane-users')
    $('#users-refresh').unbind('click').bind 'click', () =>
      @showUsersInfo()
    tbody = $('#users-table tbody')
    @oauth.rest '', '/rest/admin/users/list?', null, (err, data) =>
      if err then return @showError err
      tbody.empty()
      for user in data?.list
        tr = $(document.createElement('tr')).appendTo(tbody)
        $(document.createElement('td')).text(user.username ? '').appendTo(tr)
        $(document.createElement('td')).text(user.name ? '').appendTo(tr)
        rights = ''
        for r in user?.rights ? []
          rights += r.charAt(0)+' '
        $(document.createElement('td')).text(rights ? '').appendTo(tr)
        td = $(document.createElement('td')).appendTo(tr)
        btn = $(document.createElement('button')).addClass('btn').appendTo(td)
        btn.text('Edit')
        do (btn, user) =>
          btn.bind 'click', () =>
            @editUser user, yes
    #, test: {list: [@user, {username: 'test', name: 'test', created: 0, id: 1}]}


  editUser: (info, admin) ->
    if admin
      @showPane('main-users', 'pane-user-info')
      @showBreadcrumbs [{caption: 'Users', handler: () =>
        @showUsersInfo()
      }, {caption: info.username}]
    else
      @showPane('main-user-info', 'pane-user-info')
      @showBreadcrumbs [{caption: info.username}]
    nameEdit = $('#user-info-name')
    nameEdit.val(info.name ? '').focus()
    emailEdit = $('#user-info-email')
    emailEdit.val(info.email ? '')
    $('#user-info-username').text(info.username)
    $('#user-info-created').text(new Date(info.created).format(@DATE_FORMAT))
    $('#user-info-save').unbind('click').bind 'click', () =>
      info.name = nameEdit.val()
      info.email = emailEdit.val()
      @oauth.rest '', '/rest/admin/users/info/update?', JSON.stringify(info), (err, data) =>
        if err then return @showError err
        @editUser data, admin
      #, test: info
      return no
    tbody = $('#tokens-table tbody')
    tbody.empty()
    dt = new Date().getTime()
    @oauth.rest '', "/rest/admin/tokens/list?id=#{info.id}&", null, (err, data) =>
      if err then return @showError err
      for item in data.list
        tr = $(document.createElement('tr')).appendTo(tbody)
        $(document.createElement('td')).text(item.app ? '').appendTo(tr)
        $(document.createElement('td')).text(item.ip ? '').appendTo(tr)
        $(document.createElement('td')).text(new Date(item.created).format(@DATE_TIME_FORMAT)).appendTo(tr)
        $(document.createElement('td')).text(new Date(item.accessed).format(@DATE_TIME_FORMAT)).appendTo(tr)
        td = $(document.createElement('td')).appendTo(tr)
        btn = $(document.createElement('button')).addClass('btn btn-danger').appendTo(td)
        btn.text('Remove')
        do (btn, item) =>
          btn.bind 'click', () =>
            @showPrompt 'Are you sure want to remove token?', =>
              @oauth.rest '', '/rest/admin/tokens/remove?', JSON.stringify({token: item.token, user_id: info?.id}), (err, data) =>
                if err then return @showError err
                @editUser info, admin
              #, test: item
    #, test: {list: [{token: 'aaa', app: 'lima1', ip: '127.0.0.1', created: dt, accessed: dt}, {token: 'bbb', app: 'whiskey2', created: dt, accessed: dt}]}

  addApp: ->
    dialog = $('#add-app-dialog')
    name = $('#add-app-name').val('')
    dialog.modal(backdrop: 'static', keyboard: no)
    name.focus()
    dialog.find('#add-app-cancel').unbind('click').bind 'click', (e) =>
      dialog.modal('hide')
    dialog.find('#add-app-save').unbind('click').bind 'click', (e) =>
      app = name.val().trim().toLowerCase()
      if not app then return @showError 'Name is required'
      @oauth.rest '', '/rest/admin/apps/add?', JSON.stringify(app: app), (err, data) =>
        if err then return @showError err
        @editApp data
        dialog.modal('hide')
      #, test: {app: app, name: 'Just created app', rev: 99}

  editApp: (app) ->
    @showPane(null, 'pane-app-info')
    @showBreadcrumbs [{caption: 'Applications', handler: =>
      @showAppsInfo()
    }, {caption: app.app}]
    $('#app-info-app').text(app.app)
    $('#app-info-rev').text(app.rev ? '-')
    nameEdit = $('#app-info-name').val(app.name ? '').focus()
    descEdit = $('#app-info-desc').val(app.desc ? '')
    schemaEdit = $('#app-info-schema').val(app.schema ? '{\n}')
    $('#app-info-save').unbind('click').bind 'click', () =>
      try
        schema = JSON.parse(schemaEdit.val())
      catch e
        return @showError 'Invalid JSON provided'
      app.schema = schemaEdit.val()
      app.name = nameEdit.val()
      app.desc = descEdit.val()
      @oauth.rest '', '/rest/admin/apps/update?', JSON.stringify(app), (err, data) =>
        if err then return @showError err
        @showAppsInfo()
      #, test: app
      return no

  showAppsInfo: ->
    @showPane('main-apps', 'pane-apps')
    @showBreadcrumbs [{caption: 'Applications'}]
    $('#apps-create').unbind('click').bind 'click', () =>
      @addApp()
    $('#apps-refresh').unbind('click').bind 'click', () =>
      @showAppsInfo()
    tbody = $('#apps-table tbody')
    @oauth.rest '', '/rest/admin/apps/list?', null, (err, data) =>
      if err then return @showError err
      tbody.empty()
      for item in data?.list
        tr = $(document.createElement('tr')).appendTo(tbody)
        $(document.createElement('td')).text(item.app ? '').appendTo(tr)
        $(document.createElement('td')).text(item.name ? '').appendTo(tr)
        $(document.createElement('td')).text(item.rev ? '-').appendTo(tr)
        td = $(document.createElement('td')).appendTo(tr)
        btn = $(document.createElement('button')).addClass('btn').appendTo(td)
        btn.text('Edit')
        do (btn, item) =>
          btn.bind 'click', () =>
            @editApp item
    #, test: {list: [{id: 0, app: 'whiskey2', rev: 1}, {id: 1, app: 'sstack', name: 'StickStack application'}]}

  showDataInfo: ->
    @showPane('main-data-info', 'pane-data-info')
    @showBreadcrumbs [{caption: 'Data info'}]
    $('#data-info-refresh').unbind('click').bind 'click', () =>
      @showDataInfo()
    tbody = $('#data-info-table tbody')
    @oauth.rest '', '/rest/admin/apps/list?', null, (err, data) =>
      if err then return @showError err
      tbody.empty()
      for item in data?.list
        tr = $(document.createElement('tr')).appendTo(tbody)
        $(document.createElement('td')).text(item.app ? '').appendTo(tr)
        $(document.createElement('td')).text(item.name ? '').appendTo(tr)
        do (item) =>
          td = $(document.createElement('td')).appendTo(tr)
          btnStat = $(document.createElement('button')).addClass('btn btn-info').text('Load').appendTo(td)
          btnStat.bind 'click', () =>
            loadStat()
          statTable = $(document.createElement('table')).addClass('table table-condensed stat-table').appendTo(td)
          downloadBackup = (type) =>
            dt = new Date().format('yyyymmdd_HHMM')
            file = "#{item.app}_#{type}_#{dt}.zip"
            url = "/rest/backup?fname=#{file}&type=#{type}&"
            window.open(@oauth.getFullURL(item.app, url))
          loadStat = =>
            @oauth.rest item.app, '/rest/admin/data/stat?', null, (err, data) =>
              statTable.empty()
              addRow = (caption, value, class_caption, class_value) ->
                tr = $(document.createElement('tr')).appendTo(statTable)
                td = $(document.createElement('td')).addClass('stat-td-caption').appendTo(tr)
                td.text(caption)
                if class_caption then td.addClass(class_caption)
                td = $(document.createElement('td')).addClass('stat-td-value').appendTo(tr)
                td.text(value)
                if class_value then td.addClass(class_value)
              addRow('Objects:', data.t, 'stat-td-bold', 'stat-td-bold')
              for st in data.d ? []
                addRow("#{st.s}:", st.c)
              addRow('Files:', data.f, 'stat-td-bold', 'stat-td-bold')
              size = data.fs ? 0
              sizes = [{size: 1024, suffix: 'B'}, {size: 1024*1024, suffix: 'Kb'}, {size: 1024*1024*1024, suffix: 'Mb'}, {size: 1024*1024*1024*1024, suffix: 'Gb'}]
              sizeText = '???'
              for i in [0...sizes.length]
                sz = sizes[i]
                if size<sz.size or i is sizes.length-1
                  # Found or last
                  num = Math.round(size*10240/sz.size, 0)/10
                  sizeText = "#{num} #{sz.suffix}"
                  break
              addRow('Size:', sizeText)
            #, test: {t: 11, f: 0, fs: 1234567, d: [{s: 'notes', c: 99}, {s: 'sheets', c: 5}]}
          td = $(document.createElement('td')).appendTo(tr)
          btnBackupData = $(document.createElement('button')).addClass('btn').text('Data').appendTo(td)
          btnBackupData.bind 'click', () =>
            downloadBackup('data')
          $(document.createElement('span')).text(' ').appendTo(td)
          btnBackupFiles = $(document.createElement('button')).addClass('btn').text('Files').appendTo(td)
          btnBackupFiles.bind 'click', () =>
            downloadBackup('file')
          td = $(document.createElement('td')).appendTo(tr)
          #restoreTarget = $(document.createElement('div')).addClass('restore-target').text('DROP').appendTo(td)
          #restoreTarget.attr(title: 'Drop backups here to restore')
          files = $(document.createElement('input')).attr(type: 'file', multiple: 'multiple').addClass('data-restore-files').appendTo(td)
          files.bind 'change', (e) =>
            fileList = files.get(0)?.files
            log 'Changed:', fileList
            if fileList?.length>0
              @showPrompt 'Are you sure want to restore from backup from '+fileList?.length+' files provided? It will replace all your data', =>
                @showError 'Not implemented'
          btnRestore = $(document.createElement('button')).addClass('btn btn-warning').text('Restore').appendTo(td)
          btnRestore.bind 'click', () =>
            files.click()
          td = $(document.createElement('td')).appendTo(tr)
          btnRemove = $(document.createElement('button')).addClass('btn btn-danger').text('Clear').appendTo(td)
          btnRemove.bind 'click', () =>
            @showPrompt 'Are you sure want to remove all associated data?', =>
              @oauth.rest item.app, '/rest/admin/data/clear?', item, (err, data) =>
                if err then return @showError err
                @showAlert "Data removed for application #{item.app}"
                loadStat()
              , test: item
    #, test: {list: [{id: 0, app: 'whiskey2', rev: 1}, {id: 1, app: 'sstack', name: 'StickStack application'}]}


  showUserInfo: ->
    @editUser(@user, no)

  loadUserInfo: ->
    @oauth.rest '', '/rest/admin/users/info?', null, (err, data) =>
      if err then return @showError err
      log 'UserInfo:', data
      @initUI(data)

  logout: ->
    @showPrompt 'Really logout?', =>
      delete window?.localStorage[@SAVED_TOKEN]
      @initUI()
      @login()

  login: ->
    dialog = $('#main-login-dialog')
    username = dialog.find('#login-username')
    password = dialog.find('#login-password')
    remember = dialog.find('#login-remember')
    password.val('')
    dialog.modal(backdrop: 'static', keyboard: no)
    username.focus()
    dialog.find('#login-do-login').unbind('click').bind 'click', (e) =>
      @oauth.tokenByUsernamePassword username.val(), password.val(), (err, data) =>
        if err
          @showError(err.error_description ? err)
          password.val('')
          username.focus()
          return
        delete window?.localStorage[@SAVED_TOKEN]
        if remember.attr('checked')
          window?.localStorage[@SAVED_TOKEN] = data.access_token
        log 'Token', data, remember.attr('checked')
        dialog.modal('hide')
        @loadUserInfo()

  showError: (message) ->
    @showAlert message, severity: 'error', 'Error'

  showPrompt: (message, handler) ->
    div = $(document.createElement('p'))
    button = $(document.createElement('button')).addClass('btn btn-danger').text('Proceed').appendTo(div)
    button.bind 'click', (e) =>
      if handler then handler()
      alert.remove()
    alert = @showAlert message, persistent: yes, severity: 'block', content: div, 'Prompt'

  showAlert: (message, config, title = 'Lima1') ->
    div = $(document.createElement('div')).appendTo($('#alerts')).addClass('alert alert-'+(config?.severity ? 'info'));
    $(document.createElement('button')).appendTo(div).addClass('close').attr({'data-dismiss': 'alert'}).html('&times;');
    $(document.createElement('h4')).appendTo(div).text(title ? 'Untitled');
    $(document.createElement('span')).appendTo(div).text(message);
    if config?.content
      div.append(config.content)
    if not config?.persistent
      setTimeout =>
        div.remove();
      , config?.timeout ? 3000
    return div
