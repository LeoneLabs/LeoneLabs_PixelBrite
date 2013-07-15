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
    rgbGamma = setGamma(0, 225, 2.3, 0, 255, 2.2, 0, 200, 2.4);
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
  
  PImage getFrame(int frame_n) {
    return images[frame_n];
  }

}
