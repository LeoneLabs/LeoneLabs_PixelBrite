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
                            
    fileManager = new FileManager(10, y + ArrayWidth_textbox.getHeight()+40,(int)(app.width*0.3),app.height-70, sketchPath("")+"/PixelBrite_Patterns/");                        
  
    //animation start and stop sliders
    pixelPlayer = new PixelPlayer(app,fileManager.x+fileManager.w+20,10, (int)(app.width*0.3), app.height-70, array_width, array_height);
  
    //the pixelrecorder
    pixelRecorder = new PixelRecorder( pixelPlayer.x+pixelPlayer.w+20,10,(int)(app.width*0.3),app.height-110,array_width, array_height);
    
    
 
  }
  void display(){

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
  void updateArray(PApplet app){
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
  void controls(controlP5.ControlGroup list){
    int file_selected = int(list.value());
    if (list.name()=="Patterns") {
      String[] filenames = fileManager.listFileNames(sketchPath+"/PixelBrite_Patterns");
      String filepath = sketchPath+"/PixelBrite_Patterns/"+filenames[file_selected];  
      println(filepath);
      pixelPlayer.play_button.setOff();
      pixelPlayer.loadAnimation(filepath);
      pixelPlayer.play_button.setOn();
      pixelPlayer.remap_button.setOff();
      pixelPlayer.gamma_button.setOff();
      pixelPlayer.ghost_slider.setValue(1.0);
    }
  }
  void controls(controlP5.Controller controller){
      if (controller==ArrayWidth_textbox || controller==ArrayHeight_textbox){
      updateArray(host);
    }
  }
  
}
