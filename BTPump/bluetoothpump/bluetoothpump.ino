/* YourDuino Multiple DS18B20 Temperature Sensors on 1 wire
  Connections:
  DS18B20 Pinout (Left to Right, pins down, flat side toward you)
  - Left   = Ground
  - Center = Signal (Pin 2):  (with 3.3K to 4.7K resistor to +5 or 3.3 )
  - Right  = +5 or +3.3 V

   Questions: terry@yourduino.com 
   V1.01  01/17/2013 ...based on examples from Rik Kretzinger
   
/*-----( Import needed libraries )-----*/
#include <string.h>

// Get 1-wire Library here: http://www.pjrc.com/teensy/td_libs_OneWire.html
#include <OneWire.h>

//Get DallasTemperature Library here:  http://milesburton.com/Main_Page?title=Dallas_Temperature_Control_Library
#include <DallasTemperature.h>

#include <ble_mini.h>

/*-----( Declare Constants and Pin Numbers )-----*/
#define ONE_WIRE_BUS_PIN 2

/*-----( Declare objects )-----*/
// Setup a oneWire instance to communicate with any OneWire devices
OneWire oneWire(ONE_WIRE_BUS_PIN);

// Pass our oneWire reference to Dallas Temperature.
DallasTemperature sensors(&oneWire);


/*-----( Declare Variables )-----*/
// Assign the addresses of your 1-Wire temp sensors.
// See the tutorial on how to obtain these addresses:
// http://www.hacktronics.com/Tutorials/arduino-1-wire-address-finder.html

DeviceAddress Probe01 = { 0x28, 0x06, 0xB2, 0x53, 0x06, 0x00, 0x00, 0xE1 }; 
DeviceAddress Probe02 = { 0x28, 0xD0, 0x49, 0x60, 0x06, 0x00, 0x00, 0x81 };

// control powerSocket
const int powerPin = A0;
const int ledPin = 13;
boolean powerOn = false;
boolean override = false;

// control LEDs
byte incomingCommand[80];
char endPacket = '|';
char delimiter = ',';

int incomingBytes;  //a variable to read incoming serial data 

String pumpOneName = "HLT";
String pumpTwoName = "MashTun";

int activeTherm = 2;
int offTemperature = 152;

boolean sendData = false;

void setup()   /****** SETUP: RUNS ONCE ******/
{
  BLEMini_begin(57600);
  
  // start serial port to show results
  Serial.begin(57600);
  Serial.print("Initializing Temperature Control Library Version ");
  Serial.println(DALLASTEMPLIBVERSION);
  
  // Initialize the Temperature measurement library
  sensors.begin();
  
  // set the resolution to 10 bit (Can be 9 to 12 bits .. lower is faster)
  sensors.setResolution(Probe01, 10);
  sensors.setResolution(Probe02, 10);
  
  pinMode(ledPin, OUTPUT);
  pinMode(powerPin, OUTPUT);

  digitalWrite(ledPin, LOW);
  digitalWrite(powerPin, HIGH);

}//--(end setup )---


void printHexArray( char array[], int len )
{
  for( int i=0; i < len; i++ )
  {
     Serial.print( array[i], HEX );
     Serial.print( " " );
  }
  Serial.println();
}

byte HEADER = 0x0F;
byte TAIL = 0x0E;

byte TEMP = 0x01;
byte SET_PUMP = 0x02;
byte OVERRIDE = 0x03;

byte READ_TEMP = 0x0A;
byte READ_PUMP = 0x0B;


void loop()   /****** LOOP: RUNS CONSTANTLY ******/
{
  int avail = BLEMini_available();
  int offset = 0;

  memset( incomingCommand, 0x00, sizeof( incomingCommand ) );
  
  //Protocol
  // 0x0F Header
  
  // Commands
  // 0x01 0x00 (Query Temp )
  // 0x02 Pump# Temp (Set Active Pump)
  // 0x03 on/off (Override Stop)
  
  // Return Data
  // 0x0A Pump# Temp Pump# temp
  // 0x0B CurrentActivePump temp
  
  // 0x0E Trailer

  /* Next steps are to print out the incoming packet.  Format is off. */
  
  if( avail >= 4 )
  {    
      for( int i=0; i < avail; i++ ) {
         incomingCommand[i] = BLEMini_read();        
      }
      
      // Check Header, get Command
      for( int i=0; i < avail; i++ )
      {
        if( incomingCommand[i] == HEADER ) {
           offset = i+ 1;
           break;
        }
      }
      
      Serial.print( "Command: " );
      Serial.print( incomingCommand[offset],HEX );
      Serial.println();
  } 
  
  if( incomingCommand[offset] == TEMP ) // Temp Query
  {
    sendData = (incomingCommand[++offset] == 0x01 );
  }
  else if ( incomingCommand[offset] == SET_PUMP )
  {
    activeTherm = (int) incomingCommand[2];
    offTemperature = (int) incomingCommand[3];
    
    memset( incomingCommand, 0, sizeof( incomingCommand ) );
    incomingCommand[0] = HEADER;
    incomingCommand[1] = READ_PUMP;
    incomingCommand[2] = activeTherm;
    incomingCommand[3] = offTemperature;
    incomingCommand[4] = TAIL;

    Serial.print( "Pump " );
    Serial.print( activeTherm );
    Serial.print(  " Active - ON at < " );
    Serial.print(  offTemperature );
    Serial.print( "F \n" );
  }
  else if( incomingCommand[offset] == OVERRIDE )
  { 
    override = ( incomingCommand[++offset] == (byte) 0x01 );
    
    if( override )
    {
      digitalWrite(ledPin, LOW);
      digitalWrite(powerPin, HIGH);
      Serial.println(F("OR-OFF"));
    }
    else
    {
      digitalWrite(ledPin, HIGH);
      digitalWrite(powerPin, LOW);
      Serial.println(F("OR-ON"));     
    }
  }
  else
  {
    if( sendData )
    {
      // Command all devices on bus to read temperature  
      sensors.requestTemperatures();  

      boolean active = false;
      if( activeTherm == 1 )
      {
          active = ( DallasTemperature::toFahrenheit(sensors.getTempC(Probe01) ) < offTemperature );
        }
      else
      {
          active = ( DallasTemperature::toFahrenheit(sensors.getTempC(Probe02) ) < offTemperature );
      }

      byte pumpStatus = 0x00; // OFF
      if( active && !override)
      {
          digitalWrite(ledPin, HIGH);
          digitalWrite(powerPin, LOW);
          Serial.println(F("ON"));
          pumpStatus = 0x01;
      }
      else 
      {
          digitalWrite(ledPin, LOW);
          digitalWrite(powerPin, HIGH);
          Serial.println(F("OFF"));
      }
      
      memset( incomingCommand, 0x00, sizeof( incomingCommand ) - 9 );
      incomingCommand[0] = HEADER;
      incomingCommand[1] = READ_TEMP;
      incomingCommand[2] = 0x01;
      int temp = getTemperature(Probe01) * 100;
      Serial.print( "Probe 1: " );
      Serial.print( temp / 100 );
      Serial.println();
      incomingCommand[3] = (byte) (temp >> 8);
      incomingCommand[4] = (byte) (temp & 0xFF);
      incomingCommand[5] = 0x02;
      temp = getTemperature(Probe02) * 100;
      Serial.print( "Probe 2: " );
      Serial.print( temp / 100 );
      Serial.println();
      incomingCommand[6] = (byte) (temp >> 8);
      incomingCommand[7] = (byte) (temp & 0xFF);
      incomingCommand[8] = pumpStatus;
      incomingCommand[9] = TAIL;
    }
  }

  if( incomingCommand[0] == HEADER) {
    BLEMini_write_bytes( incomingCommand, 18 );
  }
 delay(250);
  
}//--(end main loop )---

/*-----( Declare User-written Functions )-----*/
float getTemperature(DeviceAddress deviceAddress)
{
   float tempC = sensors.getTempC(deviceAddress);

   if (tempC == -127.00) 
     return tempC;

   return DallasTemperature::toFahrenheit(tempC);
}// End printTemperature
//*********( THE END )***********

