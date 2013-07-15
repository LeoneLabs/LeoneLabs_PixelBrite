//LeoneLabs PixelPal app - July 2013
//PixelBrite project
//www.leonelabs.com
//Built in Processing 2.01
//Required libraries: controlP5, WS2801
//Derived from Adafruit WS2801 libraries and files

import controlP5.*;
import java.awt.*;
import gifAnimation.*;

//controlP5 gui
ControlP5 cp5;

//java desktop capture
Robot bot;

//Pixel Array
PixelBrite pixelBrite;

void setup() {
  size(560,400, P2D); //P2D works, JAVA2D does not
  //size(1120,800);
  noSmooth();
  pixelBrite = new PixelBrite(this, 10,10,this.height-10,120);
}

void draw() {
  background(100);
  pixelBrite.display();
}
void stop() {
  pixelBrite.pixelStreamer.disconnect();
}
void mouseDragged(){
  mousePressed();
}
void mousePressed(){
  pixelBrite.pixelRecorder.mouseEvent();
}
void controlEvent(ControlEvent theEvent) {
  if (theEvent.isController()){
    //array controllers
    pixelBrite.controls(theEvent.controller());
    pixelBrite.pixelPlayer.controls(theEvent.controller());
    pixelBrite.pixelRecorder.controls(theEvent.controller());
    pixelBrite.pixelStreamer.controls(theEvent.controller());
 
  }
  //the patternList control
  else if (theEvent.isGroup() ) {
    pixelBrite.controls(theEvent.group());
  }
}


