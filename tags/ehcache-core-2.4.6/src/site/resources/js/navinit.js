function initMenu() {
  $('#navList ul').hide();

  //$('#navList ul').filter("strong").show();
  $('#navList ul').filter(function(index) {return $('strong', this).length == 1;}).show();
  $('#navList ul').filter(function(index) {return $('strong', this).length == 1;}).parent().find('img.icon').attr('src', 'http://ehcache.org/images/arrow_open.png') ;




    $('#navList li a.parent').click(
    function() {
	
      $(this).parent().find('img.icon').attr('src', 'http://ehcache.org/images/arrow_open.png') ;

      var checkElement = $(this).next();
      if((checkElement.is('ul')) && (checkElement.is(':visible'))) {
         $(this).parent().find('img.icon').attr('src', 'http://ehcache.org/images/arrow_closed.png') ;
         checkElement.slideUp('normal');

        }
      if((checkElement.is('ul')) && (!checkElement.is(':visible'))) {
        //$('#navList ul').filter(function(index) {return $("strong", this).length == 0}).slideUp('normal');

        checkElement.slideDown('normal');
        
        }
      }
    );

  
   

    $('li:parent').css('margin', '0px');


  }



$(document).ready(function() {initMenu();});