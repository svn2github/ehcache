function g(id){
	return document.getElementById(id);
	
}


function mouseUpdate(x,y){
	
	var x = $("#marquee").offset().left + x;
	var y = $("#marquee").offset().top + y;
	
	//g('mouse').innerHTML = x + "<br />" + y + "<br />";
	
	g("popup").style.left = x +60 +"px";
	g("popup").style.top = (y-90) +"px";
	g("popup").style.display = "block";
	
}

function popupHide(){
	g("popup").style.display = "none";
	
	
}

$('#popup').mouseenter(function() {
  g("popup").style.display = "none";
});