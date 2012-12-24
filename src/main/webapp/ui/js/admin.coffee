yepnope({
  load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/admin.css', 'lib/lima1/net.js'],
  complete: ->
    $(document).ready(->
      log 'App started'
      app = new AdminApp
      app.start()
    );
})

class AdminApp
  
  SAVED_TOKEN: 'token'
  
  constructor: ->
    jqnet = new jQueryTransport 'https://lima1-kvj.rhcloud.com'
    @oauth = new OAuthProvider {
      clientID: 'lima1admin'
      scope: 'web'
    }, jqnet
    @oauth.on_token_error = =>
      @login()

  start: ->
    token = window?.localStorage[@SAVED_TOKEN]
    if token
      @oauth.token = token
      @loadUserInfo()
    else
      @login()
    
  loadUserInfo: ->
    @oauth.rest '', '/rest/admin/users/info?', null, (err, data) =>
      if err then return @showError err
      log 'UserInfo:', data
  
  login: ->
    dialog = $('#main-login-dialog')
    username = dialog.find('#login-username')
    password = dialog.find('#login-password')
    remember = dialog.find('#login-remember')
    password.val('')
    dialog.modal('show')
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

  showAlert: (message, config, title = 'Whiskey2') ->
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
