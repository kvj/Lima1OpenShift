yepnope({
  load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/admin.css', 'lib/lima1/net.js'],
  complete: ->
    $(document).ready(->
      #app = new Whiskey2
      #app.start()
    );
})

