//LeoneLabs PixelBrite v1 - June 2013
//Based on the Adafruit Adalight code - https://github.com/adafruit/Adalight - see below for licensing and description
//Compatible with Teensy 2.0 and the PixelBrite PCB v2
//Required libraries:
//SDFat.h - https://code.google.com/p/sdfatlib/downloads/list
//Adafruit_WS2801.h - https://github.com/adafruit/Adafruit-WS2801-Library
//For more information: http://www.leonelabs.com
//see footer for licensing and adafruit information

#include <SPI.h>
#include "Adafruit_WS2801.h"
#include <SdFat.h>

//**************************************LEDstream****************************//
// LED pin for Teensy:
#define LED_DDR  DDRD
#define LED_PORT PORTD
#define LED_PIN  _BV(PORTD6)

static const uint8_t magic[] = {'A','d','a'};
#define MAGICSIZE  sizeof(magic)
#define HEADERSIZE (MAGICSIZE + 3)

#define MODE_HEADER 0
#define MODE_HOLD   1
#define MODE_DATA   2

static const unsigned long serialTimeout = 1500; // 15 seconds

uint8_t
    buffer[256],
    indexIn       = 0,
    indexOut      = 0,
    mode          = MODE_HEADER,
    hi, lo, chk, i, spiFlag;
  int16_t
    bytesBuffered = 0,
    hold          = 0,
    c;
  int32_t
    bytesRemaining;
  unsigned long
    startTime,
    lastByteTime,
    lastAckTime,
    t;
//****************************SDstream******************************//
SdFat sd;
SdFile myFile;

int frame_index;

int dataPin  = 2;    //Teensy pin
int clockPin = 1;    //Teensy pin

// Set the first variable to the NUMBER of pixels. 25 = 25 pixels in a row
const int numPixels = 100;
const int NCOLORS = 3;

Adafruit_WS2801 strip = Adafruit_WS2801(numPixels);

int int0 = 5; //interrupt0 is on pin=5
volatile int state = 0;
volatile boolean open_SD = false;
boolean read_SD = false;

uint8_t pixelbuf[numPixels*NCOLORS];
char data[4] = {0,0,0,0}; //4 character max e.g. "255,"
int16_t serial_c;
//******************************************************************//

void setup()
{

  LED_DDR  |=  LED_PIN; // Enable output for LED
  LED_PORT &= ~LED_PIN; // LED off

  //Serial.begin(115200); // Teensy/32u4 disregards baud rate; is OK!

  //SPI.begin();
  //SPI.setBitOrder(MSBFIRST);
  //SPI.setDataMode(SPI_MODE0);
  //SPI.setClockDivider(SPI_CLOCK_DIV8);  // 2 MHz
  //SPI.setClockDivider(SPI_CLOCK_DIV16); 

  /*uint8_t testcolor[] = { 0, 0, 0, 255, 0, 0 };
  for(char n=3; n>=0; n--) {
    for(c=0; c<25000; c++) {
      for(i=0; i<3; i++) {
        for(SPDR = testcolor[n + i]; !(SPSR & _BV(SPIF)); );
      }
    }
    delay(1); // One millisecond pause = latch
  }*/

  //Serial.print("Ada\n"); // Send ACK string to host

  startTime    = micros();
  lastByteTime = lastAckTime = millis();
  
  //***********************************************************//
  //************************SDstream***************************//
  attachInterrupt(int0,butStateChange,CHANGE);
  
  //if (!sd.init(SPI_HALF_SPEED)) sd.initErrorHalt(); //SPI_HALF_SPEED
  if (!sd.begin()) sd.initErrorHalt();
    strip.begin(); //all the SPI initializations occur in here
  
  // set current working directory
  if (!sd.chdir("PB")) {
    sd.errorHalt("chdir failed.");
  }

  frame_index=0;
  //***********************************************************//

  for(;;) {
    switch (state){ 
    case 0: {
      startup(4); 
    }
    break;
    case 1:{ 
      rainbow(50);
    }
    break;
    case 2:{
      LEDstream();
    }
    break;
    case 3:{
      SDstream(10);
    }
    break; 
    }
  } 
}//end of setup

void loop(){}//not used

void butStateChange(){
  
  static unsigned long last_interrupt_time = 0;
  unsigned long interrupt_time = millis();
  // If interrupts come faster than 200ms, assume it's a bounce and ignore
  if (interrupt_time - last_interrupt_time > 200)
  {
    if (state<3) {
      state++;   //state gets set to zero when the SDstream reaches the end of the card
    }
    open_SD = true;
   
  }
  last_interrupt_time = interrupt_time;
 
}
// fill the dots one after the other with said color
// good for testing purposes
void colorWipe(uint32_t c, uint8_t wait) {
  int i;
  
  for (i=0; i < strip.numPixels(); i++) {
      strip.setPixelColor(i, c);
      strip.show();
      //delay(wait);
  }
}
void startup(uint8_t wait){
  int i;
  uint8_t j;
  if (frame_index<256){
    j = (uint8_t)(255*pow((float)(frame_index/255.0),2.2));
  }
  else if (frame_index<512){
    j = (uint8_t)(255*pow((float)((255-frame_index%255)/255.0),2.2));
  }
  else{
    state++;
  }
  for (i=0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, j, j, j);
  }  
  strip.show();
  delay(wait);
  frame_index++;
}
void rainbow(uint8_t wait) {
  int i, j;
  j = frame_index%256;
  for (i=0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, Wheel( (i + j) % 255));
  }  
  strip.show();   // write all the pixels out
  delay(wait);
  frame_index++;
}
/* Helper functions */
uint32_t Wheel(byte WheelPos)
{
  if (WheelPos < 85) {
   return Color(WheelPos * 3, 255 - WheelPos * 3, 0);
  } else if (WheelPos < 170) {
   WheelPos -= 85;
   return Color(255 - WheelPos * 3, 0, WheelPos * 3);
  } else {
   WheelPos -= 170; 
   return Color(0, WheelPos * 3, 255 - WheelPos * 3);
  }
}
// Create a 24 bit color value from R,G,B
uint32_t Color(byte r, byte g, byte b)
{
  uint32_t c;
  c = r;
  c <<= 8;
  c |= g;
  c <<= 8;
  c |= b;
  return c;
}

// Arduino "bridge" code between host computer and WS2801-based digital
// RGB LED pixels (e.g. Adafruit product ID #322).  Intended for use
// with USB-native boards such as Teensy or Adafruit 32u4 Breakout;
// works on normal serial Arduinos, but throughput is severely limited.
// LED data is streamed, not buffered, making this suitable for larger
// installations (e.g. video wall, etc.) than could otherwise be held
// in the Arduino's limited RAM.

// Some effort is put into avoiding buffer underruns (where the output
// side becomes starved of data).  The WS2801 latch protocol, being
// delay-based, could be inadvertently triggered if the USB bus or CPU
// is swamped with other tasks.  This code buffers incoming serial data
// and introduces intentional pauses if there's a threat of the buffer
// draining prematurely.  The cost of this complexity is somewhat
// reduced throughput, the gain is that most visual glitches are
// avoided (though ultimately a function of the load on the USB bus and
// host CPU, and out of our control).

// LED data and clock lines are connected to the Arduino's SPI output.
// On traditional Arduino boards, SPI data out is digital pin 11 and
// clock is digital pin 13.  On both Teensy and the 32u4 Breakout,
// data out is pin B2, clock is B1.  LEDs should be externally
// powered -- trying to run any more than just a few off the Arduino's
// 5V line is generally a Bad Idea.  LED ground should also be
// connected to Arduino ground.

// --------------------------------------------------------------------
//   This file is part of Adalight.

//   Adalight is free software: you can redistribute it and/or modify
//   it under the terms of the GNU Lesser General Public License as
//   published by the Free Software Foundation, either version 3 of
//   the License, or (at your option) any later version.

//   Adalight is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU Lesser General Public License for more details.

//   You should have received a copy of the GNU Lesser General Public
//   License along with Adalight.  If not, see
//   <http://www.gnu.org/licenses/>.

