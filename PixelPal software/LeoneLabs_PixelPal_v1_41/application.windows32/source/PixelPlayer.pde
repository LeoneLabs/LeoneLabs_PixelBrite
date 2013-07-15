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
    rgbGamma = setGamma(0, 255, 1.0, 0, 255, 1.0, 0, 255, 1.0);
    
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
    ghost_slider = cp5.addSlider("Ghost").setBroadcast(false).setPosition(x,offset_y).setRange(0,1.0).setWidth(100).setValue(1).setBroadcast(true);
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
  void display(){
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
  PImage applyRemapGamma(PImage input_img){
    PImage output_img = createImage(array_width,array_height,RGB);
    short r0 = 0;
    short g0 = 0;
    short b0 = 0;
    for (int y=0;y<array_height;y++){
      for (int x=0;x<array_width;x++){ 
        color c;
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

  PImage renderFrame(int n, Animation anim, PImage previous_frame){
    
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
  void resizeImage(PImage out_img, PImage in_img, int scale_x, int scale_y){  
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
  void updateBlendCount(){
    //blend_count = PApplet.parseInt(BlendCount_textbox.getText());
    blend_count = (int)(blendcount_slider.getValue());
    if (blend_count>(frame_stop-frame_start)/2){
      blend_count = (frame_stop-frame_start)/2;
      //BlendCount_textbox.setText(str(blend_count));
      blendcount_slider.setValue(blend_count);
    }
  }
   void updateSaveScaler(){
    saveScaler = PApplet.parseInt(saveScaler_textbox.getText());
    if (saveScaler < 1) saveScaler = 1;
  }
  void updateFrameRange(){
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
  void loadAnimation(String filepath){
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
      blurMax = floor(array_width*0.25); //blurMax depends on the size of the image
    }
    else{
       blurMax = floor(array_height*0.25);
    }
    preblur_slider.setBroadcast(false).setRange(0,blurMax).setNumberOfTickMarks(blurMax+1).setBroadcast(true);
    
    delay_slider.setValue(0);
    remap_button.setOff();
    ghost_slider.setValue(1.0);
    gamma_button.setOff();
    play_button.setOn();
    blend_count = 0;
   
  }
  //load from buffer (recorded)
  void loadAnimation(byte[] pixel_buffer){
    
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
      blurMax = floor(array_width*0.25); //blurMax depends on the size of the image
    }
    else{
       blurMax = floor(array_height*0.25);
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
  

 
  
  void savePNG(String fileName){
      PImage save_frame = display_animation.getFrame(frame_n);
      PImage resized_save_frame = createImage(array_width*saveScaler,array_height*saveScaler,RGB);
      resizeImage(resized_save_frame,save_frame,saveScaler);
    //PImage save_frame = resizeImage(,saveScaler);
    
      resized_save_frame.save(fileName+".png");
  }

  void saveBytesToFile(String fileName){
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
            color c;
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

  void controls(controlP5.Controller controller){

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
        rgbGamma = setGamma(0, 225, 2.3, 0, 255, 2.2, 0, 200, 2.4);
      }
      else{
        rgbGamma = setGamma(0, 255, 1.0, 0, 255, 1.0, 0, 255, 1.0);
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
  void saveAnimationToFile(File selection) { //callback function for selectOutput()
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
  void saveGIFToFile(File selection){ //callback function for selectOutput()
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
  void openGIF(File selection){ //callback function for selectOutput()
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
  void savePNGToFile(File selection){ //callback function for selectOutput()
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
  
  PImage sampleImage(PImage source_image) {
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
  PImage sampleImageVer2(PImage source_image) {
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

