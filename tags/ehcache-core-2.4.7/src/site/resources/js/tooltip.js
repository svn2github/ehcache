function g(id){
	return document.getElementById(id);
}


function mouseUpdate(x,y, title){
	
	var x = $("#marquee").offset().left + x;
	var y = $("#marquee").offset().top + y;
	
	g("popup").style.left = x +20 +"px";
	g("popup").style.top = (y-25) +"px";
   if(g("popup").style.display == "none"){
	    $("#popup").fadeIn(100);
   }
   




      g("popupContent").innerHTML = "";
      g("popupContent").innerHTML = g(title).innerHTML;
    


}

function popupHide(){
    if(g("popup").style.display != "none"){
        $("#popup").fadeOut(100);

   }


}

$('#popupTitle').mouseover(function() {

       alert();


});





function mouseX(evt) {if (!evt) evt = window.event; if (evt.pageX) return evt.pageX;

else if (evt.clientX)return evt.clientX + (document.documentElement.scrollLeft ?  document.documentElement.scrollLeft : document.body.scrollLeft); else return 0;

}


function mouseY(evt) {if (!evt) evt = window.event; if (evt.pageY) return evt.pageY; else if (evt.clientY)return evt.clientY + (document.documentElement.scrollTop ? document.documentElement.scrollTop : document.body.scrollTop); else return 0;}

function follow(evt) {
	if (document.getElementById) {
        var obj = document.getElementById('popup').style;
        //obj.visibility = 'visible';

		obj.left = (parseInt(mouseX(evt))+20) + 'px';
		obj.top = (parseInt(mouseY(evt))-25) + 'px';
	}

}


document.onmousemove = follow;

$('#Wrapper').mouseover(
    function() {
      popupHide();

});