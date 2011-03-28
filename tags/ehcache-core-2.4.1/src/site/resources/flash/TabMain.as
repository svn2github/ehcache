package  
{

	import flash.events.*;
	import flash.display.*;
	import flash.text.*;
	import tabslider;
	import Tester;
	import otherDemo;
	import homepage;
	import flash.net.URLRequest;
	import flash.display.Loader;
	import flash.events.Event;
	import flash.events.ProgressEvent;
	import flash.utils.*;
	import flash.system.*;
	import fl.transitions.Tween;
	import fl.transitions.easing.*;
	import fl.transitions.TweenEvent;
	
	/**
	 * ...
	 * @author Benjamin Hoad for Terracotta
	 */
	public class TabMain extends MovieClip
	{
		
		public function TabMain() : void
		{
			
			var titleString:String = root.loaderInfo.parameters.titles;
			var contentString:String = root.loaderInfo.parameters.content;
			top.mask = tab;
			var target = 0;
			var Twidth:int = 115;
			var pageTarget:int = 0;
			var loading:Boolean = false;
			var contentCount:int = 0;
			stage.addEventListener(Event.ENTER_FRAME, repeat); 
			function repeat (myevent:Event):void { 
				
					tab.x = (tab.x + target + tab.width/2)/2 ;
					tab.width = tab.width + (Twidth - tab.width);
					
			}
			
			
			
			
			/*function promoOver(event:MouseEvent):void {
			  target = promo.x - promo.width/2;
			  Twidth = promo.width;
			}*/
			
		
			//initialise sprites
			var main:Sprite=new Sprite();
			main.x=0;
			main.y=56;
			main.mask = stagemask;
			
			
			
			
			//Create Titles
			var titleArray:Array = titleString.split(',');
			trace('test');
			var contentArray:Array = contentString.split(',');
			trace('test');
			var leftX:int = 5;
			
			for (var i:int; i < titleArray.length; i++) {
				
				
				
				
				var upperText:TextField = new TextField();
				upperText.text = titleArray[i];
				top.addChild(upperText);
				upperText.x = leftX ;
				upperText.y = 6;
				upperText.selectable = false;
				upperText.autoSize = TextFieldAutoSize.LEFT;
				
				
				var lowerText:TextField = new TextField();
				lowerText.text = titleArray[i];
				darkLayer.addChild(lowerText);
				lowerText.x = leftX ;
				lowerText.y = 4;
				lowerText.selectable = false;
				lowerText.autoSize = TextFieldAutoSize.LEFT;
				
				
				
				
				
				var upper:TextFormat = new TextFormat();  
				upper.color = 0xFFFFFF;   
				upper.size = 26;  
				upper.bold = true;  
				upper.font = "Arial";
				
				
				
				
				var lower:TextFormat = new TextFormat();  
				lower.color = 0x000000;   
				lower.size = 16;  
				lower.bold = true;  
				lower.font = "Arial";
				
				
				lowerText.setTextFormat(lower); 
				upperText.setTextFormat(upper);  
				
				var overlay:Sprite = new Sprite();
				var g:Graphics = overlay.graphics;
				g.beginFill(0xFF0000, 0);
				g.drawRect(0, 0, lowerText.width +20, 32);
				g.endFill();
				overlay.x = lowerText.x-5;
				overlay.y = lowerText.y;
				overlay.tabIndex = i;
				
				if (i == 0) {
					target = overlay.x - overlay.width/2;
					Twidth = overlay.width;
				}
				
				if (i == titleArray.length -1) {
					white.x = overlay.x + overlay.width;
				}
				
				addChild(overlay);
				
				overlay.addEventListener(MouseEvent.MOUSE_OVER, onOver);
				loading = true;
				function onOver (e:MouseEvent) {
					
					target = e.target.x - e.target.width/2;
				  Twidth = e.target.width;
				  pageTarget = e.target.tabIndex * 650 ;
				  
				  
				  var myTweenX:Tween = new Tween(main, "alpha", Strong.easeOut, main.alpha, 0, 0.5, true);
					myTweenX.addEventListener(TweenEvent.MOTION_FINISH, doNextTween);
				  
				  
				function doNextTween(e:TweenEvent):void {
					main.x = - pageTarget;
					var myTweenAlpha:Tween = new Tween(main, "alpha", Strong.easeOut, main.alpha, 1, 0.2, true);
					
					}	
					
				
				}
				
				
				/*
				var mLoader:Loader = new Loader();
				var myRequest:URLRequest = new URLRequest(contentArray[contentCount]);
				mLoader.contentLoaderInfo.addEventListener(Event.COMPLETE, onCompleteHandler);
				mLoader.load(myRequest);

				function onCompleteHandler(loadEvent:Event)
				{
						var page:Sprite = new Sprite();
				
						main.addChild(page);
						page.addChild(loadEvent.currentTarget.content);
						var content:MovieClip = loadEvent.currentTarget.content;
						content.y += 10;
						content.scaleY = 0.8;
						content.scaleX = 0.8;
						content.x += (650 - content.width) / 2 ;
						page.x = contentCount * 650;
						contentCount++;
						loading = false;
						loadNext();
						
				}*/
				
				
				
				leftX = upperText.x + upperText.width + 25;
				
				
				
			}
			
			mainLayer.addChild(main);
			
			
			var loadingArr = new Array();
			for (var i:int; i < contentArray.length; i++) {
				loadingArr[i] = new URLRequest(contentArray[i]);
			}

			var nextItem:Number = 0;


			function showProgress(evt:Event):void {

			}



			function loadNext(){
				nextItem++;
				if (nextItem <= contentArray.length) {
					loader = new Loader();
					loader.contentLoaderInfo.addEventListener(Event.COMPLETE, completeHandler);
					loader.load(new URLRequest(contentArray[nextItem]));
						
					var page:Sprite = new Sprite();
					main.addChild(page);
					page.addChild(loader);
					page.x = nextItem * 650;
					loader.y += 10;
					loader.scaleY = 0.8;
					loader.scaleX = 0.8;
					loader.x += 50;
					
					/*
					var text:TextField = new TextField();
					text.text = "fgdfgdfgdfgdfgdfgdfgdf";
					page.addChild(text);
					text.x = 50 ;
					text.y = 40;
					text.selectable = false;
					text.autoSize = TextFieldAutoSize.CENTER;
				*/
					
					
				} else { 
					//done loading content.
					//gotoAndPlay("2",1);
				}
			}

		

			function completeHandler(event:Event):void {
			
				
				loadNext();

			}
			
			
			
			
			
			
			
			
			var loader = new Loader();
			loader.contentLoaderInfo.addEventListener(Event.COMPLETE, completeHandler);

			loader.load(new URLRequest(contentArray[nextItem]));
			
			var page:Sprite = new Sprite();
			main.addChild(page);
			page.addChild(loader);
			loader.x += 85;
			loader.y += 10;
			loader.scaleY = 0.8;
			loader.scaleX = 0.8;
			
			
		}
		
	}

}