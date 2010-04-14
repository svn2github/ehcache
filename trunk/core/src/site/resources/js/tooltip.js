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
    g("popupTitle").innerHTML = "<strong>"+ title+" Child Modules</strong>";
    g("popupTitle").style.fontSize = "15";


}

function popupHide(){
    if(g("popup").style.display != "none"){
        $("#popup").fadeOut(100);

   }

	
}

$('#popup').mouseOver(function() {

  if(g("popup").style.display != "none"){
        $("#popup").fadeOut(100);

   }
});

$('#popupTitle').mouseOver(function() {

  if(g("popup").style.display != "none"){
        $("#popup").fadeOut(100);

   }
});

$('#popupContent').mouseOver(function() {

  if(g("popup").style.display != "none"){
        $("#popup").fadeOut(100);

   }
});