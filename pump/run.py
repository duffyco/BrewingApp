# Example startup to set Probe 1 to 185> python run.py P1,185|
# NOTE: If specifying OFF - it won't go back on until "ON" is sent
# ???  IT WON'T GO TO OFF AUTOMATICALLY  SHOULD START ON

import sys
import time
import serial
import os
import pumpdef

print str( sys.argv ) 

# configure the serial connections (the parameters differs on the device you are connecting to)
ser = serial.Serial(
    port=pumpdef.PUMPTTY,
)

if( ser.isOpen() == False ):
	ser.open()
	ser.isOpen()


for i in range( 1, 10 ) :
        ser.write( "T|" )
        out = ''
        # let's wait one second before reading output (let's give device time to answer)
        time.sleep(0.1)
        while ser.inWaiting() > 0:
            out += ser.read(1)

        if out != '':
           print out
           os.system( 'clear' );

for i in range( 1, len( sys.argv ) ) : 
        ser.write( sys.argv[i] + "|" )
        out = ''
        # let's wait one second before reading output (let's give device time to answer)
        time.sleep(0.1)
        while ser.inWaiting() > 0:
            out += ser.read(1)

        if out != '':
           print "Startup Param: " + out
           os.system( 'clear' )


time.sleep(0.1)
input=1
while 1 :
        # send the character to the device
        # (note that I happend a \r\n carriage return and line feed to the characters - this is requested by my device)
        ser.write("T|")

        out = ''
        # let's wait one second before reading output (let's give device time to answer)
        time.sleep(0.5)
        while ser.inWaiting() > 0:
            out += ser.read(1)

        if out != '':
           os.system( 'clear' ) 
           print out

