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
  void saveToOutputBuffer(){
    output_buffer = new byte[byte_n];
    for (int i=0;i<byte_n;i++){
      //output_buffer[i] = buffer[remap[i]]; //make sure the remap LUT matches the layout
      output_buffer[i] = pixel_buffer[i];
    }
  }
  void display(){
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
  void updateProgressBar(){
    fill(255,0,0);
    rect(buffer_progress_bar.x,buffer_progress_bar.y,buffer_progress_bar.width,buffer_progress_bar.height);
    noFill();
    stroke(0);
    int len = (w-(rec_button.getWidth()+remove_dup_button.getWidth()+20));
    rect(buffer_progress_bar.x,buffer_progress_bar.y,len ,buffer_progress_bar.height);     
  }
  void mouseEvent(){
    captureGUI.mouseEvent();
  }
  void record(PImage sampled_img){
    
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
          color c = sampled_img.pixels[y*sampled_img.width+x];
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
  void controls(controlP5.Controller controller){
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
