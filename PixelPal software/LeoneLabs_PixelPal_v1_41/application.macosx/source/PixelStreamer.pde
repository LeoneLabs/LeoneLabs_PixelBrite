import processing.serial.*;
import ws2801.*;

class PixelStreamer{
  int x, y, w, h;
  controlP5.Button connect_button, streamON_button;
  PApplet host;
  Serial port_PixelBrite;
  WS2801 myLEDs;
  int array_width, array_height;
  
  int[] remap_LUT;
  
  PixelStreamer(PApplet app, int x, int y, int array_width, int array_height){
    host = app;
    this.x = x;
    this.y = y;
    this.array_width = array_width;
    this.array_height = array_height;
    connect_button = cp5.addButton("Connect").setSize(40,20).setPosition(x,y).setSwitch(true);
    streamON_button = cp5.addButton("Stream").setSize(40,20).setPosition(x,y+connect_button.getHeight()).setSwitch(true).setVisible(false);
    remap_LUT = zigzag(array_width, array_height, START_RIGHT | START_BOTTOM | COL_MAJOR);
    
  }
  
  void display(color[] pixel_array ){
    
    if (streamON_button.isOn() && connect_button.isOn()){
      //myLEDs.refresh(pixelBrite.pixelRecorder.captureGUI.sampled_img.pixels,remap_LUT);
      myLEDs.refresh(pixel_array,remap_LUT);
      
    }
    
  }
  void connect(){
     //port_PixelBrite = new Serial(host, Serial.list()[0], 115200);
     port_PixelBrite = WS2801.scanForPort(host);
      if (port_PixelBrite != null){
        myLEDs = new WS2801(port_PixelBrite, array_width * array_height); 
        myLEDs.setGamma(0, 225, 2.3, 0, 255, 2.2, 0, 200, 2.4);
        println("PixelBrite connected");
      }
      streamON_button.setVisible(true);
  }
  void disconnect(){
      myLEDs.dispose();
      streamON_button.setOff().setVisible(false);
      
  }
  void controls(controlP5.Controller controller){
    if (controller==connect_button){
      if (connect_button.isOn()){
        connect();
      } 
      else{
        disconnect();
      }
    }
  }
}
