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
    ghost_slider_cap = cp5.addSlider("Ghost_").setBroadcast(false).setPosition(blur_slider.getPosition().x,blur_slider.getPosition().y+30).setRange(0,1.0).setWidth(40).setValue(1);
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
  void display(){
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
  void capture(){
    
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
  PImage renderFrame(){

      if (ghost_slider_cap.getValue()<1){
        //println("blending");
        return blendFrames(sampled_img,prev_img,ghost_slider_cap.getValue());
      }
      else{
        return sampled_img;
      }
         
  }
  PImage blendFrames(PImage src_img0, PImage src_img1, float percentage){ //images need to be equal sized
    
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
        
        float r_out = percentage*r0+(1.0-percentage)*r1;
        float g_out = percentage*g0+(1.0-percentage)*g1;
        float b_out = percentage*b0+(1.0-percentage)*b1;
        
        dest_img.pixels[n] = color(r_out,g_out,b_out);
      }
    }
 
    dest_img.updatePixels();
    return dest_img;
  }
  void mouseEvent(){
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
 
  
  void grabDesktop(){
    desktop_img = new PImage(bot.createScreenCapture(desktop_rect));
  }
  
  
   
  void resizeImage(PImage out_img, PImage in_img, int scale){
    
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
  void updateCaptureWindow(int x, int y){ //x, y are relative to the PApplet, need to convert to screen coordinates
    //println("update");
    cap_rect.x = (int)((x-frame_rect.x)/(float)frame_rect.width*displayWidth);
    cap_rect.y = (int)((y-frame_rect.y)/(float)frame_rect.height*displayHeight);
    CapX_textbox.setText(str(cap_rect.x));
    CapY_textbox.setText(str(cap_rect.y));
    
    
  }
  void expandCaptureWindow(int x, int y){
    //println("expand");
    int cap_lr_x = cap_rect.x+cap_rect.width;
    int cap_lr_y = cap_rect.y+cap_rect.height;
    cap_rect.width = cap_rect.width+(x-cap_lr_x);
    cap_rect.height = cap_rect.height+(y-cap_lr_y);
  }
  
  void updateCaptureCoords(){
    cap_rect.x = PApplet.parseInt(CapX_textbox.getText());
    cap_rect.y = PApplet.parseInt(CapY_textbox.getText());
    int c_x = constrain(cap_rect.x,0,displayWidth-array_width);
    int c_y = constrain(cap_rect.y,0,displayHeight-array_height);
    cap_rect.x = c_x;
    cap_rect.y = c_y;
    CapX_textbox.setText(str(cap_rect.x));
    CapY_textbox.setText(str(cap_rect.y));
  }
  void updateCaptureScaler(){
    cap_scaler_x = PApplet.parseInt(CapScalerX_textbox.getText());
    cap_scaler_y = PApplet.parseInt(CapScalerY_textbox.getText());
    if (cap_scaler_x < 1 ) cap_scaler_x = 1;
    if (cap_scaler_y < 1 ) cap_scaler_y = 1;
    cap_rect.width = (int)(array_width*cap_scaler_x);
    cap_rect.height = (int)(array_width*cap_scaler_y);
    
  }
  PImage sampleImage(PImage source_image) {
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
  PImage histSampleImage(int histsize_x, int histsize_y, PImage source_image) {
    
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

