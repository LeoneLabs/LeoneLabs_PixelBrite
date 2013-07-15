import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import controlP5.*; 
import java.awt.*; 
import gifAnimation.*; 
import processing.serial.*; 
import ws2801.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class LeoneLabs_PixelPal_v1_41 extends PApplet {

//LeoneLabs PixelPal app - July 2013
//PixelBrite project
//www.leonelabs.com
//Built in Processing 2.01
//Required libraries: controlP5, WS2801
//Derived from Adafruit WS2801 libraries and files





//controlP5 gui
ControlP5 cp5;

//java desktop capture
Robot bot;

//Pixel Array
PixelBrite pixelBrite;

public void setup() {
  size(560,400, P2D); //P2D works, JAVA2D does not
  //size(1120,800);
  noSmooth();
  pixelBrite = new PixelBrite(this, 10,10,this.height-10,120);
}

public void draw() {
  background(100);
  pixelBrite.display();
}
public void stop() {
  pixelBrite.pixelStreamer.disconnect();
}
public void mouseDragged(){
  mousePressed();
}
public void mousePressed(){
  pixelBrite.pixelRecorder.mouseEvent();
}
public void controlEvent(ControlEvent theEvent) {
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


class Animation {
  PImage[] images;
  int imageCount;
  byte[] pixel_buffer;
  int array_width, array_height;
  
  int currentFrame, renderedFrame;
  
  short[][] rgbGamma;
  
  Animation(byte[] buffer, int array_width, int array_height) {
    this.array_width = array_width;
    this.array_height = array_height;
    rgbGamma = setGamma(0, 225, 2.3f, 0, 255, 2.2f, 0, 200, 2.4f);
    //load up buffer
    currentFrame = 0;
    pixel_buffer = buffer;
    imageCount = pixel_buffer.length/3/(array_width*array_height);
    images = new PImage[imageCount];
    //load up images
    for (int i = 0; i < imageCount; i++) {
      images[i] = createImage(array_width,array_height,RGB);
      images[i].loadPixels();
      int n=i*3*array_width*array_height;
      for (int y=0;y<array_height;y++){
        for (int x=0;x<array_width;x++){
          int red_value = (int)(pixel_buffer[n] & 0xff); //convert -127 to 127 to 0-255
          n++;
          int green_value = (int)(pixel_buffer[n] & 0xff);
          n++;
          int blue_value  = (int)(pixel_buffer[n] & 0xff);
          n++;
          images[i].pixels[y*array_width+x] = color(red_value,green_value,blue_value);  
          
        }
      }
      images[i].updatePixels();
    }
  }
  
  public PImage getFrame(int frame_n) {
    return images[frame_n];
  }

}
class CaptureGUI
{
  int x,y,w,h;
  Rectangle frame_rect;
  PVector GUIscaler; //for converting GUI coordinates to applet coordinates 
  PVector GUIoffset; //offset of the desktop capture section relative to the x,y position of the captureGUI
  Rectangle cap_rect;
  int array_width, array_height;
  
  int frame_rater, t, prev; //for calculating frame rate

  Rectangle desktop_rect;
  PImage desktop_img;
  PImage capture_img;
  PImage sampled_img;
  PImage prev_img;
  PImage display_img;
  PImage rendered_img;
  int display_scale;
  
  int cap_scaler_x;
  int cap_scaler_y;
  
  controlP5.Textfield CapX_textbox, CapY_textbox, CapScalerX_textbox, CapScalerY_textbox;
  controlP5.Slider blur_slider,
                   ghost_slider_cap;
  int blurValue;
  controlP5.Button grab_desktop_button;
  controlP5.Button sample_mode_button;
  
  int capture_x, capture_y;
  
  // Initialize capture code:
  GraphicsEnvironment ge;
  GraphicsDevice[]    gd;
  
  CaptureGUI(int x, int y, int w, int h, int array_width, int array_height){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.array_width = array_width;
    this.array_height = array_height;
    capture_x = 60;
    capture_y = 64;
    cap_scaler_x = 20;
    cap_scaler_y = 20;
    blurValue = 0;
    
    ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    gd = ge.getScreenDevices();
    try {
      bot = new Robot(gd[0]);
    }
    catch(AWTException e) {
      System.err.println("new Robot() failed.");
    }
  
    CapX_textbox = cp5.addTextfield("X");
    CapX_textbox.setSize(25,20);
    CapX_textbox.setPosition(x,y);
    CapX_textbox.setText(str(capture_x));
    CapX_textbox.setAutoClear(false);
    
    CapY_textbox = cp5.addTextfield("Y");
    CapY_textbox.setSize(25,20);
    CapY_textbox.setPosition(x+CapX_textbox.getWidth()+10,y);
    CapY_textbox.setText(str(capture_y));
    CapY_textbox.setAutoClear(false);
    
    CapScalerX_textbox = cp5.addTextfield("W");
    CapScalerX_textbox.setSize(25,20);
    CapScalerX_textbox.setPosition(x,y+CapX_textbox.getHeight()+20);
    CapScalerX_textbox.setText(str(cap_scaler_x));
    CapScalerX_textbox.setAutoClear(false);
    
    CapScalerY_textbox = cp5.addTextfield("H");
    CapScalerY_textbox.setSize(25,20);
    CapScalerY_textbox.setPosition(x+(CapX_textbox.getWidth()+10),y+CapX_textbox.getHeight()+20);
    CapScalerY_textbox.setText(str(cap_scaler_y));
    CapScalerY_textbox.setAutoClear(false);
    grab_desktop_button = cp5.addButton("Grab Desktop").setSize(60,20).setPosition(x+(CapX_textbox.getWidth()+10)*2,y);
//    int blurMax; 
//    if (array_width<array_height){
//      blurMax = floor(array_width*0.25); //blurMax depends on the size of the image
//    }
//    else{
//       blurMax = floor(array_height*0.25);
//    }
    int blurMax = 5;
    blur_slider = cp5.addSlider("Blur").setPosition(x+(CapX_textbox.getWidth()+10)*2,grab_desktop_button.getPosition().y+grab_desktop_button.getHeight()+10)
                     .setBroadcast(false).setRange(0,blurMax).setWidth(40).setValue(0).setNumberOfTickMarks(blurMax+1)
                     .setBroadcast(true); 
    ghost_slider_cap = cp5.addSlider("Ghost_").setBroadcast(false).setPosition(blur_slider.getPosition().x,blur_slider.getPosition().y+30).setRange(0,1.0f).setWidth(40).setValue(1);
    int offset_x = CapX_textbox.getWidth()+CapY_textbox.getWidth()+grab_desktop_button.getWidth()+30;
    sample_mode_button = cp5.addButton("Hist.").setSize(25,19).setPosition(x+offset_x,y).setSwitch(true);
    
    GUIoffset = new PVector(0,(CapX_textbox.getHeight()+20)*2);
    
    //this is where the representation of the desktop is shown inside the captureGUI
    frame_rect = new Rectangle((int)(x+GUIoffset.x),(int)(y+GUIoffset.y),w,h); 

    GUIscaler = new PVector(frame_rect.width/(float)displayWidth,frame_rect.height/(float)displayHeight);

    desktop_rect = new Rectangle(0, 0, displayWidth, displayHeight);
    cap_rect = new Rectangle(capture_x,capture_y,array_width*cap_scaler_x,array_height*cap_scaler_y);
    
    if (array_width>=array_height) display_scale = (int)(w/(float)array_width);
    else display_scale = (int)((400-(frame_rect.y+frame_rect.height+10))/(float)array_height); //note the 400, thats the PApplet height, lazy coding
    display_img = createImage(display_scale*array_width,display_scale*array_height,RGB);
    desktop_img = createImage(displayWidth,displayHeight,RGB);
    grabDesktop();
    rendered_img = createImage(array_width,array_height,RGB);
    prev_img = createImage(array_width,array_height,RGB);
  }
  public void display(){
    //grab the desktop and save to sampled_img
    capture();
    //convert to GUI coordinates
    int c_x = (int)(cap_rect.x*GUIscaler.x);
    int c_y = (int)(cap_rect.y*GUIscaler.y);
    int c_w = (int)(cap_rect.width*GUIscaler.x);
    int c_h = (int)(cap_rect.height*GUIscaler.y);
    //desktop
    image(desktop_img, frame_rect.x, frame_rect.y, frame_rect.width, frame_rect.height);
    //red capture window
    noFill();
    stroke(255,0,0); //red
    rect(frame_rect.x+c_x,frame_rect.y+c_y,c_w,c_h); //inner capture window
    //captured section
    rendered_img = renderFrame();
    resizeImage(display_img,rendered_img,display_scale);
    image(display_img, frame_rect.x, frame_rect.y+frame_rect.height+10);
    
    prev_img = rendered_img;
  }
  public void capture(){
    
    //track the framerate - I can get about 60fps on Win7 with Aero turned off
    frame_rater++;
    if ((t = second()) != prev) {
      //print("Average framerate: ");
      //print(int((float)frame_rater / (float)millis() * 1000.0));
      //println(" fps");
      prev = t;
    }
    
    //captures images from the desktop and then samples to match the pixel array
    capture_img = new PImage(bot.createScreenCapture(cap_rect));
    capture_img.filter(BLUR,(int)(blur_slider.getValue()));
    //capture_img.filter(BLUR,8);
    if (sample_mode_button.isOn()){
      sampled_img = histSampleImage(cap_scaler_x,cap_scaler_y,capture_img); //sample the image based on histogram analysis
    }
    else{
      sampled_img = sampleImage(capture_img); //or just normal point sampling
      
      
    }
     //sampled_img.filter(BLUR,(int)(blur_slider.getValue())); //limited by the array width and array height, for 10x10, blurValue<3      
    
  }
  public PImage renderFrame(){

      if (ghost_slider_cap.getValue()<1){
        //println("blending");
        return blendFrames(sampled_img,prev_img,ghost_slider_cap.getValue());
      }
      else{
        return sampled_img;
      }
         
  }
  public PImage blendFrames(PImage src_img0, PImage src_img1, float percentage){ //images need to be equal sized
    
    src_img0.loadPixels();
    src_img1.loadPixels();
    
    PImage dest_img = createImage(array_width,array_height,RGB);
    dest_img.loadPixels(); //is this right?

    for (int y=0;y<src_img0.height;y++){
      for (int x=0;x<src_img0.width;x++){
        int n = src_img0.width*y+x;
        
        float r0 = (float)(src_img0.pixels[n] >> 16 & 0xFF); 
        float g0 = (float)(src_img0.pixels[n] >> 8 & 0xFF);
        float b0 = (float)(src_img0.pixels[n] & 0xFF);
        
        float r1 = (float)(src_img1.pixels[n] >> 16 & 0xFF); 
        float g1 = (float)(src_img1.pixels[n] >> 8 & 0xFF);
        float b1 = (float)(src_img1.pixels[n] & 0xFF);
        
        float r_out = percentage*r0+(1.0f-percentage)*r1;
        float g_out = percentage*g0+(1.0f-percentage)*g1;
        float b_out = percentage*b0+(1.0f-percentage)*b1;
        
        dest_img.pixels[n] = color(r_out,g_out,b_out);
      }
    }
 
    dest_img.updatePixels();
    return dest_img;
  }
  public void mouseEvent(){
    //convert from desktop screen coordinates into applet coordinates
    int c_x = (int)(cap_rect.x*GUIscaler.x);
    int c_y = (int)(cap_rect.y*GUIscaler.y);
    int c_w = (int)(cap_rect.width*GUIscaler.x);
    int c_h = (int)(cap_rect.height*GUIscaler.y);
    
   // println(c_x + " " + c_y + " " + c_w + " " + c_h);
    //in the desktop frame
    if(mouseX >= (x+GUIoffset.x) && mouseX < (x+GUIoffset.x)+w-c_w){ //(w-c_w) makes sure the box isn't drawn off the frame
      if (mouseY >= (y+GUIoffset.y) && mouseY < (y+GUIoffset.y)+h-c_h){
        //trying to do some click-drag rescaling of the rectangle
        //in the capture rectangle
        //if(mouseX>=(x+GUIoffset.x+c_x) && mouseX<(x+GUIoffset.x+c_x+c_w) ){
        //  expandCaptureWindow(mouseX,mouseY);
        //}
        //else{
          updateCaptureWindow(mouseX,mouseY);
        //}
      }
    }
  }
 
  
  public void grabDesktop(){
    desktop_img = new PImage(bot.createScreenCapture(desktop_rect));
  }
  
  
   
  public void resizeImage(PImage out_img, PImage in_img, int scale){
    
    out_img.loadPixels();
    for (int y=0; y<array_height; y++){
      for (int x=0; x<array_width; x++){
        for (int i=0; i<scale; i++){
          for (int j=0; j<scale; j++){
            out_img.pixels[scale*x+i+out_img.width*(y*scale+j)] = in_img.pixels[y*array_width+x];
          }
        }
      }
    }
    out_img.updatePixels();

  }
  public void updateCaptureWindow(int x, int y){ //x, y are relative to the PApplet, need to convert to screen coordinates
    //println("update");
    cap_rect.x = (int)((x-frame_rect.x)/(float)frame_rect.width*displayWidth);
    cap_rect.y = (int)((y-frame_rect.y)/(float)frame_rect.height*displayHeight);
    CapX_textbox.setText(str(cap_rect.x));
    CapY_textbox.setText(str(cap_rect.y));
    
    
  }
  public void expandCaptureWindow(int x, int y){
    //println("expand");
    int cap_lr_x = cap_rect.x+cap_rect.width;
    int cap_lr_y = cap_rect.y+cap_rect.height;
    cap_rect.width = cap_rect.width+(x-cap_lr_x);
    cap_rect.height = cap_rect.height+(y-cap_lr_y);
  }
  
  public void updateCaptureCoords(){
    cap_rect.x = PApplet.parseInt(CapX_textbox.getText());
    cap_rect.y = PApplet.parseInt(CapY_textbox.getText());
    int c_x = constrain(cap_rect.x,0,displayWidth-array_width);
    int c_y = constrain(cap_rect.y,0,displayHeight-array_height);
    cap_rect.x = c_x;
    cap_rect.y = c_y;
    CapX_textbox.setText(str(cap_rect.x));
    CapY_textbox.setText(str(cap_rect.y));
  }
  public void updateCaptureScaler(){
    cap_scaler_x = PApplet.parseInt(CapScalerX_textbox.getText());
    cap_scaler_y = PApplet.parseInt(CapScalerY_textbox.getText());
    if (cap_scaler_x < 1 ) cap_scaler_x = 1;
    if (cap_scaler_y < 1 ) cap_scaler_y = 1;
    cap_rect.width = (int)(array_width*cap_scaler_x);
    cap_rect.height = (int)(array_width*cap_scaler_y);
    
  }
  public PImage sampleImage(PImage source_image) {
    PImage sampled_image;
  
    sampled_image = createImage(array_width, array_height, RGB);
  
    int n=0;
    int inc_x=source_image.width/sampled_image.width;
    int inc_y=source_image.height/sampled_image.height;
  
    for (int y=0;y<sampled_image.height;y++) {
      for (int x=0;x<sampled_image.width;x++) {
  
        sampled_image.pixels[x+sampled_image.width*y]=source_image.pixels[x*inc_x+source_image.width*y*inc_y];
      }
    }
  
    return sampled_image;
  }
  //subsample the image
  public PImage histSampleImage(int histsize_x, int histsize_y, PImage source_image) {
    
    PImage sampled_image;
    sampled_image = createImage(array_width, array_height, RGB);
    float r_pixel, g_pixel, b_pixel;
    
    for (int y=0;y<array_height;y++) {
      for (int x=0;x<array_width;x++) {
  
        //int n = source_image.width*y_LUT[y]+x_LUT[x];
        int n = source_image.width*y*histsize_y+x*histsize_x;
  
        //this region will calculate the histogram at each corner of the pixel and then assign the output pixel the bin with the highest count
        int[][] hist = new int[3][256];
        int[] hist_max = {0, 0, 0}; //stores the bin count
        int[] hist_max_val = {0, 0, 0}; //stores the color intensity in the bin with the highest count
        
        //grabs the correct pixel from the screen capture  
        for (int i=0; i<histsize_y; i++) {
          for (int k=0;k<histsize_x;k++) {
            int m = n+i*source_image.width+k;
            int r = (int)(source_image.pixels[m] >> 16 & 0xFF); //I'm using ints because byte is -127-127
            int g = (int)(source_image.pixels[m] >> 8 & 0xFF);
            int b = (int)(source_image.pixels[m] & 0xFF);
  
            hist[0][r]++;
            hist[1][g]++;
            hist[2][b]++;
  
            if (hist[0][r]>hist_max[0]) {
              hist_max[0]=hist[0][r]; 
              hist_max_val[0]=r;
            }
            if (hist[1][g]>hist_max[1]) {
              hist_max[1]=hist[1][g];  
              hist_max_val[1]=g;
            }
            if (hist[2][b]>hist_max[2]) {
              hist_max[2]=hist[2][b];  
              hist_max_val[2]=b;
            }
          }
        }
        r_pixel = hist_max_val[0];
        g_pixel = hist_max_val[1];
        b_pixel = hist_max_val[2];
  
        //r_pixel = pow(r_pixel/255, gamma)*255;
        //g_pixel = pow(g_pixel/255, gamma)*255;
        //b_pixel = pow(b_pixel/255, gamma)*255;
  
        sampled_image.pixels[x+sampled_image.width*y] = color(r_pixel, g_pixel, b_pixel);
        
      }
    }
  
    return sampled_image;
  }
  
}

class FileManager{
  
    //pattern files, names, and listings
    controlP5.ListBox PatternList;
    File[] PatternFiles;
    String[] PatternNames;
    String filePath;
    int x, y, w, h;
  
  FileManager(int x, int y, int w, int h, String path){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    //String path;
    PatternList = cp5.addListBox("Patterns", x, y, w, h);
    PatternList.setItemHeight(15);
    PatternList.setBarHeight(15);
  
    PatternList.captionLabel().toUpperCase(true);
    PatternList.captionLabel().set("Patterns");
    PatternList.captionLabel().style().marginTop = 3;
    PatternList.valueLabel().style().marginTop = 3; // the +/- sign
  
    PatternList.setColorBackground(color(64));
    PatternList.setColorActive(color(0, 0, 255, 128));
  
    println(path);
    PatternNames = listFileNames(path);
    PatternFiles = listFiles(path);
    PatternList.clear();
    if (PatternFiles != null && PatternNames != null){
      for (int i = 0; i < PatternFiles.length; i++) {
          PatternList.addItem(PatternNames[i], i);
      }  
    }
    
  }
  
  // This function returns all the files in a directory as an array of File objects
  public File[] listFiles(String dir) {
    File file = new File(dir);
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      return files;
    } 
    else {
      // If it's not a directory
      println("listFile=NO");
      return null;
    }
  }
  
  // This function returns all the files in a directory as an array of Strings  
  
  public String[] listFileNames(String dir) {
    File file = new File(dir);
    if (file.isDirectory()) {
      String names[] = file.list();
      return names;
    } 
    else {
      // If it's not a directory
      println("listFileNames=NO");
      return null;
    }
  }
  
  public void AddToListBox(String path, String[] filenames, File[] files) { //refreshes the list box once a file is saved
  
    filenames = listFileNames(path);    //this isn't actually loading the string[] into pattern_filenames
    files = listFiles(path);
    PatternList.clear();
    for (int i=0;i<files.length;i++) {
      PatternList.addItem(filenames[i], i);
    }
  
  }
  
  public byte[] loadPatternFromFile(String filepath){
    // Open a file and read its binary data 
    byte b[] = loadBytes(filepath); 
    //byte a[] = new byte[b.length]; 
    // Print each value, from 0 to 255 
    for (int i = 0; i < b.length; i++) {  
      // bytes are from -128 to 127, this converts to 0 to 255 
      int a = b[i] & 0xff; 
      //print(a + " "); 
    } 
    // Print a blank line at the end 
    println(); 
    return b;
  }
}
class PixelBrite{
  int x, y;
  int width, height;
  
  FileManager fileManager;  
  
  //pixelpattern
  Textfield ArrayWidth_textbox, ArrayHeight_textbox;
  int array_width = 10;
  int array_height = 10;
  
  //Pixel Recorder
  PixelRecorder pixelRecorder;

  //Pixel Player
  PixelPlayer pixelPlayer;

  //Pixel Streamer
  PixelStreamer pixelStreamer;
 
  PApplet host; 
  PixelBrite(PApplet app, int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.width = w;
    this.height = h;
    host = app;
    cp5 = new ControlP5(app); //make sure this is before pixelBrite is initializedArrayWidth_textbox = cp5.addTextfield("Width")
    ArrayWidth_textbox = cp5.addTextfield("Width")
                            .setSize(30,20)
                            .setAutoClear(false)
                            .setText(str(array_width))
                            .setPosition(x,y);
    ArrayHeight_textbox = cp5.addTextfield("Height")
                            .setSize(30,20)
                            .setAutoClear(false)
                            .setText(str(array_height))
                            .setPosition(x+ArrayWidth_textbox.getWidth()+10,y); 
    
    //pixel streamer
    pixelStreamer = new PixelStreamer(app ,x+(ArrayWidth_textbox.getWidth()+10)*2,y, array_width, array_height);
                            
    fileManager = new FileManager(10, y + ArrayWidth_textbox.getHeight()+40,(int)(app.width*0.3f),app.height-70, sketchPath("")+"/PixelBrite_Patterns/");                        
  
    //animation start and stop sliders
    pixelPlayer = new PixelPlayer(app,fileManager.x+fileManager.w+20,10, (int)(app.width*0.3f), app.height-70, array_width, array_height);
  
    //the pixelrecorder
    pixelRecorder = new PixelRecorder( pixelPlayer.x+pixelPlayer.w+20,10,(int)(app.width*0.3f),app.height-110,array_width, array_height);
    
    
 
  }
  public void display(){

      pixelPlayer.display();
      pixelRecorder.display();
      if (pixelPlayer.stream_button.isOn()){
        pixelStreamer.display(pixelPlayer.display_frame.pixels);
      }
      else{
        if (pixelStreamer.streamON_button.isOn()){
          pixelStreamer.display(pixelRecorder.captureGUI.sampled_img.pixels);
        }
      }
  }
  public void updateArray(PApplet app){
    String inputTextX = ArrayWidth_textbox.getText();
    String inputTextY = ArrayHeight_textbox.getText();
    array_width = PApplet.parseInt(inputTextX);
    array_height = PApplet.parseInt(inputTextY);

    int set_x, set_y, set_w, set_h;
    cp5 = null;
    cp5 = new ControlP5(app);
    set_x = pixelStreamer.x;
    set_y = pixelStreamer.y;
    pixelStreamer = null;
    pixelStreamer = new PixelStreamer(app ,set_x,set_y, array_width, array_height);
    set_x = pixelPlayer.x;
    set_y = pixelPlayer.y;
    set_w = pixelPlayer.w;
    set_h = pixelPlayer.h;
    pixelPlayer = null;
    pixelPlayer = new PixelPlayer(app, set_x,set_y, set_w, set_h, array_width, array_height);
    set_x = pixelRecorder.x;
    set_y = pixelRecorder.y;
    set_w = pixelRecorder.w;
    set_h = pixelRecorder.h;
    pixelRecorder = null;
    pixelRecorder = new PixelRecorder(set_x,set_y,set_w, set_h, array_width, array_height);
    
  }
  public void controls(controlP5.ControlGroup list){
    int file_selected = PApplet.parseInt(list.value());
    if (list.name()=="Patterns") {
      String[] filenames = fileManager.listFileNames(sketchPath+"/PixelBrite_Patterns");
      String filepath = sketchPath+"/PixelBrite_Patterns/"+filenames[file_selected];  
      println(filepath);
      pixelPlayer.play_button.setOff();
      pixelPlayer.loadAnimation(filepath);
      pixelPlayer.play_button.setOn();
      pixelPlayer.remap_button.setOff();
      pixelPlayer.gamma_button.setOff();
      pixelPlayer.ghost_slider.setValue(1.0f);
    }
  }
  public void controls(controlP5.Controller controller){
      if (controller==ArrayWidth_textbox || controller==ArrayHeight_textbox){
      updateArray(host);
    }
  }
  
}
//public in order to use the callback function in selectOutput()
public class PixelPlayer{
  int x,y,w,h;
  
  //controls
  controlP5.Slider frame_start_slider, 
                   frame_stop_slider,
                   timeline_slider,
                   delay_slider,
                   preblur_slider,
                   blendcount_slider;
  controlP5.Slider ghost_slider;
  controlP5.Button play_button, 
                   save_anim_button,
                   remap_button, 
                   save_png_button, 
                   gamma_button, 
                   stream_button, 
                   save_GIF_button,
                   open_GIF_button;
                   
  Textfield saveScaler_textbox;
  int saveScaler;
  
  Textfield BlendCount_textbox;
  int offset_y = 0; //for aligning elements
  
  //frames
  int frame_start;
  int frame_stop;
  int frame_n;
  int delay_count;
  
  int array_width, array_height;

  //animation
  Animation display_animation;
  
  PImage display_frame;
  PImage prev_frame;
  PImage resized_frame;
  
  int pixel_scaler;
  
  //blending
  int blend_count;
  int blend_frame;
  float perc;

  //lut
  int[] remap_LUT;
  
  GifMaker gifExport;
  PApplet host;
  
  short[][] rgbGamma;
  
  PixelPlayer(PApplet app, int x, int y, int w, int h,  int array_width, int array_height){
    host = app;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.array_width = array_width;
    this.array_height = array_height;
    frame_n=0;
    frame_start = 0;
    saveScaler = 16; //multipler for saving pixel patterns to pngs and gifs
    rgbGamma = setGamma(0, 255, 1.0f, 0, 255, 1.0f, 0, 255, 1.0f);
    
    remap_LUT = zigzag(array_width, array_height, START_RIGHT | START_BOTTOM | COL_MAJOR);
  
    saveScaler_textbox = cp5.addTextfield("Out Scale")
                            .setSize(30,20)
                            .setAutoClear(false)
                            .setText(str(saveScaler))
                            .setPosition(x,y);
                            
    save_png_button = cp5.addButton("Save PNG").setSize(50,20).setPosition(x+saveScaler_textbox.getWidth()+10,y);
   
    save_GIF_button = cp5.addButton("Save GIF").setSize(45,20).setPosition(x+(saveScaler_textbox.getWidth()+20)*2,y);
    offset_y = (int)(saveScaler_textbox.getPosition().y + saveScaler_textbox.getHeight()+25);                         
    
    blendcount_slider = cp5.addSlider("Blend").setPosition(x,offset_y);
    offset_y = (int)(offset_y + blendcount_slider.getHeight()+10);
    
    frame_start_slider = cp5.addSlider("Start").setPosition(x,offset_y); 
    
//    BlendCount_textbox = cp5.addTextfield("Blend")
//                            .setSize(25,20)
//                            .setAutoClear(false)
//                            .setText(str(blend_count))
//                            .setPosition(frame_start_slider.getPosition().x+frame_start_slider.getWidth()+40,offset_y);   
    
    offset_y = (int)(offset_y + frame_start_slider.getHeight());
    
    frame_stop_slider = cp5.addSlider("Stop").setPosition(x,offset_y);  
    offset_y = (int)(offset_y + frame_stop_slider.getHeight());
    
    timeline_slider = cp5.addSlider("Time").setPosition(x,offset_y);
    //timeline_slider.setSliderMode(Slider.FLEXIBLE); //this causes a null pointer exception i think because the controlp5.event is fired
    offset_y = (int)(offset_y + timeline_slider.getHeight()+10);
    
    delay_slider = cp5.addSlider("Delay").setBroadcast(false).setPosition(x,offset_y).setRange(0,100).setBroadcast(true);
    offset_y = (int)(offset_y + delay_slider.getHeight()+10);
    preblur_slider = cp5.addSlider("PreBlur").setBroadcast(false).setPosition(x,offset_y).snapToTickMarks(true).setNumberOfTickMarks(2).setBroadcast(true);
    offset_y = (int)(offset_y + preblur_slider.getHeight()+10);
    ghost_slider = cp5.addSlider("Ghost").setBroadcast(false).setPosition(x,offset_y).setRange(0,1.0f).setWidth(100).setValue(1).setBroadcast(true);
    offset_y = (int)(offset_y + ghost_slider.getHeight()+10);
    
    play_button = cp5.addButton("Play").setBroadcast(false).setSize(40,20).setPosition(x,offset_y).setSwitch(true);
    save_anim_button = cp5.addButton("Save").setSize(40,20).setPosition(x+play_button.getWidth()+10,offset_y); 
    stream_button = cp5.addButton("Stream2PB").setSize(60,20).setPosition(save_anim_button.getPosition().x+save_anim_button.getWidth()+10,offset_y);
   
    offset_y = (int)(offset_y + save_anim_button.getHeight()+10);
    
    gamma_button = cp5.addButton("Gamma").setSize(40,20).setPosition(x,offset_y).setSwitch(true);
    
    remap_button = cp5.addButton("Remap").setSize(40,20).setPosition(gamma_button.getPosition().x+gamma_button.getWidth()+10,offset_y).setSwitch(true);
    //this is a work-around to a null pointer exception. by calling setBroadcast(false) and then setBroadcast(true) I can avoid it
    //it has something to do with the controlEvent() method
    //this could easily apply to any of these controlp5 objects
    //http://www.sojamo.de/libraries/controlP5/reference/controlP5/Controller.html
    open_GIF_button = cp5.addButton("Open GIF").setSize(50,20).setPosition(remap_button.getPosition().x+remap_button.getWidth()+10,offset_y);
    offset_y = (int)(offset_y+open_GIF_button.getHeight()+10); 
    
    if (array_width>=array_height) pixel_scaler = (int)(w/(float)array_width);
    else pixel_scaler = (int)((app.height-offset_y)/(float)array_height); 
     
 
    display_frame = createImage(array_width,array_height,RGB);
    prev_frame = createImage(array_width,array_height,RGB);
    resized_frame = createImage(array_width*pixel_scaler,array_height*pixel_scaler,RGB);
    blend_count = 0;
    
    
  }
  public void display(){
    //outer frame
    
    PVector upper_left = new PVector(x-10, y-10);
    int player_width = w+20;
    int player_height = offset_y+h+10;
    noFill();
    stroke(0);
    rect(upper_left.x,upper_left.y,player_width,player_height);
    //scrub the frame
    if (timeline_slider.isMousePressed()){
      boolean restart = play_button.isOn();
      play_button.setOff();
      frame_n = (int)timeline_slider.getValue();
      display_frame = applyRemapGamma(renderFrame(frame_n,display_animation,prev_frame));
      if (restart) play_button.setOn();
    }
    
    if (play_button.isOn() && display_animation != null){
      
        display_frame = applyRemapGamma(renderFrame(frame_n,display_animation,prev_frame));
        prev_frame = renderFrame(frame_n,display_animation,prev_frame);
        if (delay_count<delay_slider.getValue()){
          delay_count++;
        }
        else{
          frame_n++;  //increment frame number
          delay_count = 0;
        }
        if (frame_n>frame_stop-blend_count-1) { //frame blending effectively reduces the number of frames in the loop
          frame_n=frame_start; //rewind back to start
        }   
            
    } 
    else{
      delay_count = 0;
    }
    //draw the image to the screen
  
    resizeImage(resized_frame,display_frame,pixel_scaler);
 
    image(resized_frame, x, offset_y,array_width*pixel_scaler,array_height*pixel_scaler);
    //save img to rendered animation 
    timeline_slider.setValue(frame_n);
  }
  public PImage applyRemapGamma(PImage input_img){
    PImage output_img = createImage(array_width,array_height,RGB);
    short r0 = 0;
    short g0 = 0;
    short b0 = 0;
    for (int y=0;y<array_height;y++){
      for (int x=0;x<array_width;x++){ 
        int c;
          if (remap_button.isOn())
            c = input_img.pixels[remap_LUT[y*array_width+x]];
          else
            c = input_img.pixels[y*array_width+x];
        
        int r_i = (int)(c >> 16 & 0xFF); 
        int g_i = (int)(c >> 8 & 0xFF);
        int b_i = (int)(c & 0xFF);
        
        r0 = rgbGamma[r_i][0];
        g0 = rgbGamma[g_i][1];
        b0 = rgbGamma[b_i][2]; 
        
        output_img.pixels[array_width*y+x] = color(r0,g0,b0);
      }
    }
    output_img.updatePixels();
    return output_img;
  }

  public PImage renderFrame(int n, Animation anim, PImage previous_frame){
    
      PImage out_img = createImage(array_width,array_height,RGB);  

      if ((n-frame_start)<(blend_count)){  
        PImage img0 = anim.getFrame(n);
        blend_frame = (frame_stop-blend_count)+(n-frame_start);
        PImage img1 = anim.getFrame(blend_frame);
        perc = (n-frame_start)/(float)blend_count;
        out_img = blendFrames(img0,img1,perc);
      }
      else{
        blend_frame = -1; //not sure why this is -1
        out_img = anim.getFrame(n);
      }
      
      if (ghost_slider.getValue()<1){
        out_img = blendFrames(out_img,previous_frame,ghost_slider.getValue());
      }
      return out_img;    
  }
  public PImage blendFrames(PImage src_img0, PImage src_img1, float percentage){ //images need to be equal sized
    
    src_img0.loadPixels();
    src_img1.loadPixels();
    
    PImage dest_img = createImage(array_width,array_height,RGB);
    dest_img.loadPixels(); //is this right?

    for (int y=0;y<src_img0.height;y++){
      for (int x=0;x<src_img0.width;x++){
        int n = src_img0.width*y+x;
        
        float r0 = (float)(src_img0.pixels[n] >> 16 & 0xFF); 
        float g0 = (float)(src_img0.pixels[n] >> 8 & 0xFF);
        float b0 = (float)(src_img0.pixels[n] & 0xFF);
        
        float r1 = (float)(src_img1.pixels[n] >> 16 & 0xFF); 
        float g1 = (float)(src_img1.pixels[n] >> 8 & 0xFF);
        float b1 = (float)(src_img1.pixels[n] & 0xFF);
        
        float r_out = percentage*r0+(1.0f-percentage)*r1;
        float g_out = percentage*g0+(1.0f-percentage)*g1;
        float b_out = percentage*b0+(1.0f-percentage)*b1;
        
        dest_img.pixels[n] = color(r_out,g_out,b_out);
      }
    }
 
    dest_img.updatePixels();
    return dest_img;
  }
   public void resizeImage(PImage out_img, PImage in_img, int scale){  
    out_img.loadPixels();
    for (int y=0; y<array_height; y++){
      for (int x=0; x<array_width; x++){
        for (int i=0; i<scale; i++){
          for (int j=0; j<scale; j++){
            out_img.pixels[scale*x+i+out_img.width*(y*scale+j)] = in_img.pixels[y*array_width+x];
          }
        }
      }
    }
    out_img.updatePixels();
  }
  public void resizeImage(PImage out_img, PImage in_img, int scale_x, int scale_y){  
    out_img.loadPixels();
    for (int y=0; y<in_img.height; y++){
      for (int x=0; x<in_img.width; x++){
        for (int i=0; i<scale_x; i++){
          for (int j=0; j<scale_y; j++){
            out_img.pixels[scale_x*x+i+out_img.width*(y*scale_y+j)] = in_img.pixels[y*in_img.width+x];
          }
        }
      }
    }
    out_img.updatePixels();
  }
  public void updateBlendCount(){
    //blend_count = PApplet.parseInt(BlendCount_textbox.getText());
    blend_count = (int)(blendcount_slider.getValue());
    if (blend_count>(frame_stop-frame_start)/2){
      blend_count = (frame_stop-frame_start)/2;
      //BlendCount_textbox.setText(str(blend_count));
      blendcount_slider.setValue(blend_count);
    }
  }
   public void updateSaveScaler(){
    saveScaler = PApplet.parseInt(saveScaler_textbox.getText());
    if (saveScaler < 1) saveScaler = 1;
  }
  public void updateFrameRange(){
    if (display_animation!=null){
      frame_start = (int)frame_start_slider.getValue();
      frame_stop = (int)frame_stop_slider.getValue();
      if (frame_start>frame_n){
        frame_n = frame_start;
        timeline_slider.setValue(frame_n);
        
      }
      updateBlendCount();
    }
  }

  //load from file
  public void loadAnimation(String filepath){
    if (display_animation != null) display_animation = null;
    byte[] pixel_buffer = loadBytes(filepath);
    display_animation = new Animation(pixel_buffer, array_width, array_height);
    frame_n = 0;
    //this sequence will call the updateFrameRange() from the ControlP5 events section
    frame_start_slider.setRange(0,display_animation.imageCount-1);
    frame_start_slider.setValue(0);
    frame_stop_slider.setRange(0,display_animation.imageCount);
    frame_stop_slider.setValue(display_animation.imageCount);
    
    timeline_slider.setRange(0,display_animation.imageCount-1);
   
    prev_frame = display_animation.getFrame(frame_stop-1);
    
    blendcount_slider.setRange(0,display_animation.imageCount);
    //update blendcount
//    if (smooth_button.isOn()){
//      blend_count = display_animation.imageCount/2;
//      BlendCount_textbox.setText(str(blend_count));
//    }

    int blurMax; 
    if (array_width<array_height){
      blurMax = floor(array_width*0.25f); //blurMax depends on the size of the image
    }
    else{
       blurMax = floor(array_height*0.25f);
    }
    preblur_slider.setBroadcast(false).setRange(0,blurMax).setNumberOfTickMarks(blurMax+1).setBroadcast(true);
    
    delay_slider.setValue(0);
    remap_button.setOff();
    ghost_slider.setValue(1.0f);
    gamma_button.setOff();
    play_button.setOn();
    blend_count = 0;
   
  }
  //load from buffer (recorded)
  public void loadAnimation(byte[] pixel_buffer){
    
    if (display_animation != null) display_animation = null;
    display_animation = new Animation(pixel_buffer, array_width, array_height);
    frame_n = 0;
    //this sequence will call the updateFrameRange() from the ControlP5 events section
    frame_start_slider.setRange(0,display_animation.imageCount-1);
    frame_start_slider.setValue(0);
    frame_stop_slider.setRange(0,display_animation.imageCount);
    frame_stop_slider.setValue(display_animation.imageCount);
    
    int blurMax; 
    if (array_width<array_height){
      blurMax = floor(array_width*0.25f); //blurMax depends on the size of the image
    }
    else{
       blurMax = floor(array_height*0.25f);
    }
    preblur_slider.setBroadcast(false).setRange(0,blurMax).setNumberOfTickMarks(blurMax+1).setBroadcast(true);
    blendcount_slider.setRange(0,display_animation.imageCount);
    timeline_slider.setRange(0,display_animation.imageCount-1);
    prev_frame = display_animation.getFrame(frame_stop-1);
    delay_slider.setValue(0);
    remap_button.setOff();
    play_button.setOn();
    blend_count = 0;
  }
  

 
  
  public void savePNG(String fileName){
      PImage save_frame = display_animation.getFrame(frame_n);
      PImage resized_save_frame = createImage(array_width*saveScaler,array_height*saveScaler,RGB);
      resizeImage(resized_save_frame,save_frame,saveScaler);
    //PImage save_frame = resizeImage(,saveScaler);
    
      resized_save_frame.save(fileName+".png");
  }

  public void saveBytesToFile(String fileName){
    byte[] outputBuffer = new byte[(frame_stop-blend_count-frame_start)*(int)(delay_slider.getValue()+1)*(array_width*array_height)*3];
    PImage prior_frame = createImage(array_width,array_height,RGB);
    PImage out_frame = createImage(array_width,array_height,RGB);
    int n=0;

    for (int i=frame_start;i<frame_stop-blend_count;i++){
      if (i==frame_start) prior_frame = display_animation.getFrame(i);
      for (int j=0;j<=delay_slider.getValue();j++){
        out_frame = renderFrame(i,display_animation,prior_frame);
        prior_frame = out_frame; 
        out_frame.loadPixels();
        for (int y=0;y<array_height;y++){
          for (int x=0;x<array_width;x++){  
            int c;
            if (remap_button.isOn())
              c = out_frame.pixels[remap_LUT[y*out_frame.width+x]];
            else
              c = out_frame.pixels[y*out_frame.width+x];
            outputBuffer[n] = (byte)rgbGamma[(c >> 16 & 0xFF)][0]; //red
            n++;
            outputBuffer[n] = (byte)rgbGamma[(c >> 8 & 0xFF)][1]; //green
            n++;
            outputBuffer[n] = (byte)rgbGamma[(c & 0xFF)][2]; //blue
            n++;
          }
        }
      }
    } 
    saveBytes(fileName,outputBuffer);
  }

  public void controls(controlP5.Controller controller){

    if (controller==frame_start_slider ||controller==frame_stop_slider){
      updateFrameRange(); 
    }
    if (controller == preblur_slider){
      if (display_animation != null){
      play_button.setOff();
      loadAnimation(display_animation.pixel_buffer);
      }
    }
    //smooth text box
    if (controller==blendcount_slider){
      updateBlendCount();

    }
    if (controller==saveScaler_textbox){
      updateSaveScaler();

    }
    //
    if (controller==gamma_button){
      //play_button.setOff();
      if (gamma_button.isOn()){
        rgbGamma = setGamma(0, 225, 2.3f, 0, 255, 2.2f, 0, 200, 2.4f);
      }
      else{
        rgbGamma = setGamma(0, 255, 1.0f, 0, 255, 1.0f, 0, 255, 1.0f);
      }
    }

    //save animation button
    if (controller==save_anim_button){
      play_button.setOff();
      File select_file = new File(sketchPath("")+"/PixelBrite_Patterns");
      selectOutput("Save animation to file", "saveAnimationToFile",select_file,this); //saveAnimationToFile = callback function
      
    }
    //save to PNG button
    if (controller==save_png_button){
      play_button.setOff();
      File select_file = new File(sketchPath(""));
      selectOutput("Save PNG to file", "savePNGToFile",select_file,this);
    }
    if (controller==save_GIF_button){
      play_button.setOff();
      File select_file = new File(sketchPath(""));
      selectOutput("Save GIF to file", "saveGIFToFile",select_file,this);
      
    }
    if (controller==open_GIF_button){
      play_button.setOff();
      File select_file = new File(sketchPath(""));
      selectInput("Open GIF file", "openGIF",select_file,this);
      
    }
    
  }
  public void saveAnimationToFile(File selection) { //callback function for selectOutput()
    String path;
    if (selection == null) {
      println("Window was closed or the user hit cancel.");
    } else {
      println("User selected " + selection.getAbsolutePath()); 
      path = selection.getAbsolutePath();
      pixelBrite.pixelPlayer.saveBytesToFile(path);
      //TODO: delete file if already exists
      pixelBrite.fileManager.AddToListBox(sketchPath+"/PixelBrite_Patterns",pixelBrite.fileManager.PatternNames,pixelBrite.fileManager.PatternFiles);
      loadAnimation(path);
    } 
  }
  public void saveGIFToFile(File selection){ //callback function for selectOutput()
    if (selection == null) {
      println("Window was closed or the user hit cancel.");
    } else {  
      PImage out_frame; 
      PImage prior_frame = createImage(array_width,array_height,RGB);
      PImage resized_out_frame = createImage(array_width*saveScaler,array_height*saveScaler,RGB);
      gifExport = new GifMaker(host, "temp.gif",100); //100 = quality, default = 10
      gifExport.setSize(array_width*saveScaler,array_height*saveScaler);
      gifExport.setRepeat(0); // make it an "endless" animation
      for (int i=frame_start;i<frame_stop-blend_count;i++){
        if (i==frame_start) prior_frame = display_animation.getFrame(i);
        for (int j=0;j<=delay_slider.getValue();j++){
          out_frame = applyRemapGamma(renderFrame(i,display_animation, prior_frame));
          resizeImage(resized_out_frame,out_frame,saveScaler);
          gifExport.setDelay(0);
          gifExport.addFrame(resized_out_frame);
          prior_frame = out_frame;
        }
      }
      gifExport.finish();
      File file = new File(sketchPath("") + "temp.gif");
      boolean ret =  file.renameTo(new File(selection.getAbsolutePath()+".gif"));
      if (ret) println("GIF saved");
      else println("error");
      file.delete();
    }
      
  }
  public void openGIF(File selection){ //callback function for selectOutput()
  if (selection == null) {
      println("Window was closed or the user hit cancel.");
    } else {
       File file = new File(selection.getAbsolutePath());
       boolean ret =  file.renameTo(new File(sketchPath("") + "temp.gif"));
       if (ret) println("GIF opened");
       else println("error");
       PImage[] GIF_images;
       
       GIF_images = Gif.getPImages(host,"temp.gif");

       byte[] pixel_buffer = new byte[GIF_images.length*3*array_width*array_height];
       int byte_n = 0;
       for (int i=0; i<GIF_images.length; i++){
         PImage sampled_img = sampleImage(GIF_images[i]); //sample the GIF
          for (int y=0;y<array_height;y++) {
            for (int x=0;x<array_width;x++) {
              int m = sampled_img.width*y+x;
              pixel_buffer[byte_n] = (byte)(sampled_img.pixels[m] >> 16 & 0xFF); //red
              byte_n++;
              pixel_buffer[byte_n] = (byte)(sampled_img.pixels[m] >> 8 & 0xFF); //green
              byte_n++;
              pixel_buffer[byte_n] = (byte)(sampled_img.pixels[m] & 0xFF); //blue
              byte_n++;
            }
          }
       }
      loadAnimation(pixel_buffer);
      
      file = new File(sketchPath("") + "temp.gif");
      ret =  file.renameTo(new File(selection.getAbsolutePath()));
      if (ret) println("GIF saved");
      else println("error");
      file.delete();
    }
  }
  public void savePNGToFile(File selection){ //callback function for selectOutput()
   String path;
   if (selection == null) {
    println("Window was closed or the user hit cancel.");
   } 
   else {
    println("User selected " + selection.getAbsolutePath()); 
    path = selection.getAbsolutePath();
    pixelBrite.pixelPlayer.savePNG(path);
    //TODO: delete file if already exists
   }
  }
  
  public PImage sampleImage(PImage source_image) {
    PImage sampled_image;
    sampled_image = createImage(array_width,array_height, RGB);
    int ovs_x, ovs_y;
    //oversample the source image first to create a integer multiple of the sampled image
    int magic_no0 = 1;//10
    int magic_no1 = 1;//2
    if (sampled_image.width>source_image.width && sampled_image.height>source_image.height){
      ovs_x = (int)(magic_no0*sampled_image.width/(float)(source_image.width));
      ovs_y = (int)(magic_no0*sampled_image.height/(float)(source_image.height));
    }
    else{
      ovs_x = (int)(magic_no1*source_image.width/(float)(sampled_image.width));
      ovs_y = (int)(magic_no1*source_image.height/(float)(sampled_image.height));
    }
    
    PImage oversampled_img = createImage(ovs_x*source_image.width,ovs_y*source_image.height,RGB);
    println("ovsx: " + ovs_x + " ovsy " + ovs_y);
    if(display_animation!=null)
      source_image.filter(BLUR,(int)(preblur_slider.getValue()));

    resizeImage(oversampled_img,source_image,ovs_x,ovs_y);
    //println("resize successful");
    //oversampled_img.resize(sampled_image.width,sampled_image.height);
    int inc_x=oversampled_img.width/sampled_image.width;
    int inc_y=oversampled_img.height/sampled_image.height;
    for (int y=0;y<sampled_image.height;y++) {
      for (int x=0;x<sampled_image.width;x++) {
        sampled_image.pixels[x+sampled_image.width*y]=oversampled_img.pixels[ovs_x+x*inc_x+oversampled_img.width*(y*inc_y+ovs_y)];
      }
    }

    return sampled_image;
//    return oversampled_img;
  }
  public PImage sampleImageVer2(PImage source_image) {
    PImage sampled_image;
  
    sampled_image = createImage(array_width, array_height, RGB);
  
    int n=0;
    int inc_x=source_image.width/sampled_image.width;
    int inc_y=source_image.height/sampled_image.height;
  
    for (int y=0;y<sampled_image.height;y++) {
      for (int x=0;x<sampled_image.width;x++) {
  
        sampled_image.pixels[x+sampled_image.width*y]=source_image.pixels[x*inc_x+source_image.width*y*inc_y];
      }
    }
  
    return sampled_image;
  }
}

class PixelRecorder{
  int checksum;
  int frame_count;
  int BUFFER_SIZE = 5000000;
  byte[] pixel_buffer = new byte[BUFFER_SIZE]; //(number of frames)*(number of pixels)*(number of colors)
  byte[] output_buffer;
  int byte_n = 0;
  PImage prev_img;
  PImage black_img;
  
  Rectangle buffer_progress_bar; 
  
  controlP5.Button rec_button, remove_dup_button;
  controlP5.Textlabel buffer_gauge;
  int x,y,w,h;
  
  CaptureGUI captureGUI;
  
  int array_width, array_height;
 
  PixelRecorder(int x, int y, int w, int h, int array_width, int array_height){
    this.w = w;
    this.h = h;
    this.x = x;
    this.y = y;
    this.array_width = array_width;
    this.array_height = array_height;
    checksum = 0;
    frame_count = 0;
    prev_img = createImage(array_width,array_height,RGB);
    
    rec_button = cp5.addButton("Rec").setSize(30,20).setPosition(x,y).setSwitch(true);
    buffer_progress_bar = new Rectangle((int)rec_button.getPosition().x+rec_button.getWidth(),y,0,18);
    buffer_gauge = cp5.addTextlabel("Buffer").setPosition(x+rec_button.getWidth()+10,y+4).setText(str(frame_count));
    int len = (w-(rec_button.getWidth()+40+20));
    remove_dup_button = cp5.addButton("Singles").setSize(40,20)
                           .setPosition(buffer_gauge.getPosition().x+len,y).setSwitch(true);
     //the capture gui helper
    int capture_size_x = displayWidth/8;
    int capture_size_y = displayHeight/8;
    captureGUI = new CaptureGUI(x,y+ rec_button.getHeight()+10,capture_size_x,capture_size_y, array_width, array_height);
    
    black_img = createImage(array_width,array_height,RGB);
  }
  public void saveToOutputBuffer(){
    output_buffer = new byte[byte_n];
    for (int i=0;i<byte_n;i++){
      //output_buffer[i] = buffer[remap[i]]; //make sure the remap LUT matches the layout
      output_buffer[i] = pixel_buffer[i];
    }
  }
  public void display(){
    //get screencapture
    captureGUI.display();
    //update the progressbar
    updateProgressBar();
    
    if (rec_button.isOn()){
      PImage frame = captureGUI.sampled_img;
      record(frame);
    }
    else{
      //rec_button.setOff();
      byte_n = 0;
      frame_count=0;
      buffer_progress_bar.width = 0;
      prev_img = black_img;
    }
    if (byte_n>BUFFER_SIZE-1000){//why 1000?
      rec_button.setOff();
    }
    
  }
  public void updateProgressBar(){
    fill(255,0,0);
    rect(buffer_progress_bar.x,buffer_progress_bar.y,buffer_progress_bar.width,buffer_progress_bar.height);
    noFill();
    stroke(0);
    int len = (w-(rec_button.getWidth()+remove_dup_button.getWidth()+20));
    rect(buffer_progress_bar.x,buffer_progress_bar.y,len ,buffer_progress_bar.height);     
  }
  public void mouseEvent(){
    captureGUI.mouseEvent();
  }
  public void record(PImage sampled_img){
    
    //println("recording");
    checksum = 0;
    //the screen is sampled asynchronously from the display rate, so duplicates are created
    //this removes them by counting the number of duplicates and then compares the sum to the number of columns below
    sampled_img.loadPixels();
    if (remove_dup_button.isOn()){
      for (int y=0;y<sampled_img.height;y++) {
        for (int x=0;x<sampled_img.width;x++) {
          int m = sampled_img.width*y+x;
          
          int r = (int)(sampled_img.pixels[m] >> 16 & 0xFF); //I'm using ints because byte is -127-127
          int g = (int)(sampled_img.pixels[m] >> 8 & 0xFF);
          int b = (int)(sampled_img.pixels[m] & 0xFF);
          int r_prev = (int)(prev_img.pixels[m] >> 16 & 0xFF);
          int g_prev = (int)(prev_img.pixels[m] >> 8 & 0xFF);
          int b_prev = (int)(prev_img.pixels[m] & 0xFF);
          
          if ((r==r_prev) && (g==g_prev) && (b==b_prev))
          {
            checksum++; //if checksum = width*height then the whole image is the same
          }
          else
          {
            checksum = 0; //reset because the frame is different
          }
        }
      }
    }
    //checksum will be less than the number of pixels if one pixel is different in the frame
    if (checksum==0 || checksum<sampled_img.height*sampled_img.width) { //checksum to remove duplicates from output file
      //outputs the data to buffer
      for (int y=0;y<sampled_img.height;y++) {
        for (int x=0;x<sampled_img.width;x++) {
          int c = sampled_img.pixels[y*sampled_img.width+x];
          pixel_buffer[byte_n] = (byte)(c >> 16 & 0xFF); //red
          byte_n++;
          pixel_buffer[byte_n] = (byte)(c >> 8 & 0xFF); //green
          byte_n++;
          pixel_buffer[byte_n] = (byte)(c & 0xFF); //blue
          byte_n++;
        }
      }
      frame_count++;
      buffer_progress_bar.width = (int)(byte_n/(float)pixel_buffer.length*80);
      buffer_gauge.setText(str(frame_count));
    }
    
    //store the frame for comparison
    prev_img = sampled_img;
  }
  public void controls(controlP5.Controller controller){
    //capture x and capture y textboxes
    if (controller==captureGUI.CapX_textbox || controller==captureGUI.CapY_textbox){
       captureGUI.updateCaptureCoords(); 
    }
    //capture scaler text box
    if (controller==captureGUI.CapScalerX_textbox || controller == captureGUI.CapScalerY_textbox){
       captureGUI.updateCaptureScaler(); 
    }
    //record button
    if (controller==rec_button){
      if (!rec_button.isOn()) {
        saveToOutputBuffer();
        pixelBrite.pixelPlayer.loadAnimation(output_buffer); 
        buffer_gauge.setText(str(0));
        
      }
    }
    //captureGUI controls
    if (controller==captureGUI.grab_desktop_button){
       captureGUI.grabDesktop();
    }
  }
}



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
  
  public void display(int[] pixel_array ){
    
    if (streamON_button.isOn() && connect_button.isOn()){
      //myLEDs.refresh(pixelBrite.pixelRecorder.captureGUI.sampled_img.pixels,remap_LUT);
      myLEDs.refresh(pixel_array,remap_LUT);
      
    }
    
  }
  public void connect(){
     //port_PixelBrite = new Serial(host, Serial.list()[0], 115200);
     port_PixelBrite = WS2801.scanForPort(host);
      if (port_PixelBrite != null){
        myLEDs = new WS2801(port_PixelBrite, array_width * array_height); 
        myLEDs.setGamma(0, 225, 2.3f, 0, 255, 2.2f, 0, 200, 2.4f);
        println("PixelBrite connected");
      }
      streamON_button.setVisible(true);
  }
  public void disconnect(){
      myLEDs.dispose();
      streamON_button.setOff().setVisible(false);
      
  }
  public void controls(controlP5.Controller controller){
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
//pulled these functions out of the WS2801 library from Adafruit
// Constants used for 'order' parameter of zigzag method:
public static final int
	  START_TOP    = 0,
	  START_BOTTOM = 1,
	  START_LEFT   = 0,
	  START_RIGHT  = 2,
	  ROW_MAJOR    = 0,
	  COL_MAJOR    = 4;

public int[] zigzag(int width, int height, int order) {
		int i, major, minor, incMajor, incMinor, mulMajor, mulMinor,
		    limitMajor, limitMinor;
		int remap[] = new int[width * height];

		// Determine initial position, incs, muls and limits
		if((order & COL_MAJOR) != 0) {
			mulMajor = 1;
			mulMinor = width;
			if((order & START_RIGHT) != 0) {
				major      = width - 1;
				limitMajor = -1;
			} else {
				major      = 0;
				limitMajor = width;
			}
			minor = ((order & START_BOTTOM) != 0) ? height - 1 : 0;
			limitMinor = height;
		} else { // Row major
			mulMajor = width;
			mulMinor = 1;
			if((order & START_BOTTOM) != 0) {
				major      = height - 1;
				limitMajor = -1;
			} else {
				major      = 0;
				limitMajor = height;
			}
			minor = ((order & START_RIGHT) != 0) ? width - 1 : 0;
			limitMinor = width;
		}
		incMajor = (major > 0) ? -1 : 1;
		incMinor = (minor > 0) ? -1 : 1;

		// Iterate though each position in grid, reversing
		// row/column directions as suited to the given order.
		for(i=0; major != limitMajor; i++) {
			remap[i] = major * mulMajor + minor * mulMinor;
			minor   += incMinor;
			if((minor == -1) || (minor == limitMinor)) {
				incMinor = -incMinor;
				minor   +=  incMinor;
				major   +=  incMajor;
			}
		}

		return remap;
}
// Fancy gamma correction; separate R,G,B ranges and exponents:
public short[][] setGamma(
    int rMin, int rMax, double rGamma,
    int gMin, int gMax, double gGamma,
    int bMin, int bMax, double bGamma) {
 
    short  i;
    double rRange, gRange, bRange, d;
    short[][] outGamma = new short[256][3];
    rRange = (double)(rMax - rMin);
    gRange = (double)(gMax - gMin);
    bRange = (double)(bMax - bMin);

    for(i=0; i<256; i++) {
      d = (double)i / 255.0f;
      outGamma[i][0] = (short)(rMin +
        (int)Math.floor(rRange * Math.pow(d,rGamma) + 0.5f));
      outGamma[i][1] = (short)(gMin +
        (int)Math.floor(gRange * Math.pow(d,gGamma) + 0.5f));
      outGamma[i][2] = (short)(bMin +
        (int)Math.floor(bRange * Math.pow(d,bGamma) + 0.5f));
      //println(i + " " + outGamma[i][0]);
    }
    return outGamma;
  }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "LeoneLabs_PixelPal_v1_41" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
