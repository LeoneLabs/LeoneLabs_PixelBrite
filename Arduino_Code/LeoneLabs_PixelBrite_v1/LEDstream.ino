//taken from Adafruit Adalight Arduino code
//https://github.com/adafruit/Adalight
//see below for licensing and Adafruit description
void LEDstream(){
    // Implementation is a simple finite-state machine.
    // Regardless of mode, check for serial input each time:
    t = millis();
    if((bytesBuffered < 256) && ((c = Serial.read()) >= 0)) {
      buffer[indexIn++] = c;
      bytesBuffered++;
      lastByteTime = lastAckTime = t; // Reset timeout counters
    } 
    else {
      // No data received.  If this persists, send an ACK packet
      // to host once every second to alert it to our presence.
      if((t - lastAckTime) > 1000) {
        Serial.print("Ada\n"); // Send ACK string to host
        lastAckTime = t; // Reset counter
      }
      // If no data received for an extended time, turn off all LEDs.
      if((t - lastByteTime) > serialTimeout) {
        for(c=0; c<32767; c++) {
          for(SPDR=0; !(SPSR & _BV(SPIF)); );
        }
        delay(1); // One millisecond pause = latch
        lastByteTime = t; // Reset counter
      }
    }
    switch(mode) {

     case MODE_HEADER:

      // In header-seeking mode.  Is there enough data to check?
      if(bytesBuffered >= HEADERSIZE) {
        // Indeed.  Check for a 'magic word' match.
        for(i=0; (i<MAGICSIZE) && (buffer[indexOut++] == magic[i++]););
        if(i == MAGICSIZE) {
          // Magic word matches.  Now how about the checksum?
          hi  = buffer[indexOut++];
          lo  = buffer[indexOut++];
          chk = buffer[indexOut++];
          if(chk == (hi ^ lo ^ 0x55)) {
            // Checksum looks valid.  Get 16-bit LED count, add 1
            // (# LEDs is always > 0) and multiply by 3 for R,G,B.
            bytesRemaining = 3L * (256L * (long)hi + (long)lo + 1L);
            bytesBuffered -= 3;
            spiFlag        = 0;         // No data out yet
            mode           = MODE_HOLD; // Proceed to latch wait mode
          } else {
            // Checksum didn't match; search resumes after magic word.
            indexOut  -= 3; // Rewind
          }
        } // else no header match.  Resume at first mismatched byte.
        bytesBuffered -= i;
      }
      break;

     case MODE_HOLD:

      // Ostensibly "waiting for the latch from the prior frame
      // to complete" mode, but may also revert to this mode when
      // underrun prevention necessitates a delay.

      if((micros() - startTime) < hold) break; // Still holding; keep buffering

      // Latch/delay complete.  Advance to data-issuing mode...
      LED_PORT &= ~LED_PIN;  // LED off
      mode      = MODE_DATA; // ...and fall through (no break):

     case MODE_DATA:

      while(spiFlag && !(SPSR & _BV(SPIF))); // Wait for prior byte
      if(bytesRemaining > 0) {
        if(bytesBuffered > 0) {
          SPDR = buffer[indexOut++];   // Issue next byte
          bytesBuffered--;
          bytesRemaining--;
          spiFlag = 1;
        }
        // If serial buffer is threatening to underrun, start
        // introducing progressively longer pauses to allow more
        // data to arrive (up to a point).
        if((bytesBuffered < 32) && (bytesRemaining > bytesBuffered)) {
          startTime = micros();
          hold      = 100 + (32 - bytesBuffered) * 10;
          mode      = MODE_HOLD;
	}
      } else {
        // End of data -- issue latch:
        startTime  = micros();
        hold       = 1000;        // Latch duration = 1000 uS
        LED_PORT  |= LED_PIN;     // LED on
        mode       = MODE_HEADER; // Begin next header search
      }
    } // end switch
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
//   <http://www.gnu.org/licenses/>
