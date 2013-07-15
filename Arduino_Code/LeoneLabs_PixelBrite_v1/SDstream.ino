void SDstream(uint8_t wait){
    if (open_SD){
      //close the currently open file to prepare to open the next one
      if (myFile.isOpen()) myFile.close();
      if (myFile.openNext(sd.vwd(), O_READ)){
          Serial.print("Opened "); 
          myFile.printName(&Serial);
          Serial.println();
          read_SD = true;        
      }
      else {
        sd.vwd()->rewind();
        state=1;
        Serial.println("Can't open file"); 
      }
      open_SD=false;
      frame_index=0;
    }
    
    if (read_SD){
      //read bytes
      if (numPixels*NCOLORS != myFile.read(pixelbuf,numPixels*NCOLORS)){  //For success read() returns the number of bytes read. A value less than nbyte, including zero, will be returned if end of file is reached. If an error occurs, read() returns -1. Possible errors include read() called before a file has been opened, corrupt file system or an I/O error occurred.
        //end of the file
        frame_index=0;
        myFile.rewind();
      }
      else{
          setPixels(pixelbuf, wait); //load up the pixels
          frame_index++;
      }
    }
}
void setPixels(uint8_t pixels[], uint8_t wait){
  int i;
  int j=0;
  for ( i=0; i < strip.numPixels(); i++) {
      strip.setPixelColor(i, Color(pixels[j],pixels[j+1],pixels[j+2]));
      j+=3 ;
    }  
    strip.show();   // write all the pixels out
    delay(wait);
}
