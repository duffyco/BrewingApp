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
char incomingCommand[80];
char endPacket = '|';
char delimiter = ',';

int incomingBytes;  //a variable to read incoming serial data 

String pumpOneName = "HLT";
String pumpTwoName = "MashTun";

int activeTherm = 2;
int offTemperature = 152;

void setup()   /****** SETUP: RUNS ONCE ******/
{
  // start serial port to show results
  Serial.begin(9600);
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

void loop()   /****** LOOP: RUNS CONSTANTLY ******/
{
  if( Serial.available() < 0 )
  {
     return;
  }
  
  incomingBytes = Serial.readBytesUntil( endPacket, incomingCommand, 80 );
  
  if( incomingBytes < 0 )
  {
     return;
  }
  
  // I've got something like X,Y|
  // N,Name1,Name2
  // P1,155
  // P2,155
  // T
  // ON
  // OFF
  String command( incomingCommand );

  //char* cmd = strtok( incomingCommand, delimiter ); 
  
  if( command.startsWith( "T" ) )
  {
    // Command all devices on bus to read temperature  
    sensors.requestTemperatures();  
    
    Serial.print(pumpOneName);
    Serial.print( ": " );
    printTemperature(Probe01);    
    Serial.print( " - " );
    Serial.print(pumpTwoName);
    Serial.print( " : " );
    printTemperature(Probe02);
    Serial.print( " ======= " );
    Serial.print( "Probe Active : " );
    Serial.print( activeTherm );
    Serial.print( " @ < " );
    Serial.print( offTemperature );
    Serial.print( "F" );
    Serial.println();
    
    boolean active = false;
    if( activeTherm == 1 )
    {
        active = ( DallasTemperature::toFahrenheit(sensors.getTempC(Probe01) ) < offTemperature );
    }
    else
    {/*
        Serial.print (DallasTemperature::toFahrenheit(sensors.getTempC(Probe02) ));
        Serial.print ( " - " );
        Serial.print( offTemperature );
        Serial.println();
     */
        active = ( DallasTemperature::toFahrenheit(sensors.getTempC(Probe02) ) < offTemperature );
    }
    
    if( active && !override)
    {
        digitalWrite(ledPin, HIGH);
        digitalWrite(powerPin, LOW);
        Serial.println(F("ON"));
    }
    else 
    {
        digitalWrite(ledPin, LOW);
        digitalWrite(powerPin, HIGH);
        Serial.println(F("OFF"));
    }
  }
  else if (command.startsWith( "N" ) )
  {
     command = command.substring( 2, command.length() );
     pumpOneName = command.substring( 0, command.indexOf( ',' ) );
     pumpTwoName = command.substring( command.lastIndexOf( ',' ) + 1, command.length()  );
     Serial.print( "Names set to: " );
     Serial.print( pumpOneName );
     Serial.print( " - " );
     Serial.print( pumpTwoName );
     Serial.println();
  }
  else if (command.startsWith( "P1" ) )
  {
    offTemperature = command.substring( command.indexOf( ",")+1, command.length() ).toInt();
    activeTherm = 1;
    Serial.print( "Pump 1 Active - ON at < " );
    Serial.print(  offTemperature );
    Serial.print( "F \n" );
  }
  else if (command.startsWith( "P2" ) )
  {
    offTemperature = command.substring( command.indexOf( ",")+1, command.length() ).toInt();
    activeTherm = 2;
    Serial.print( "Pump 2 Active - ON at < " );
    Serial.print(  offTemperature );
    Serial.print( "F \n" );   
  }
  else if (command.startsWith( "ON" ) )
  {
      override = false;
      digitalWrite(ledPin, HIGH);
      digitalWrite(powerPin, LOW);
      Serial.println(F("ON"));     
  }
  else if (command.startsWith( "OFF" ) )
  {
      override = true;
      digitalWrite(ledPin, LOW);
      digitalWrite(powerPin, HIGH);
      Serial.println(F("OFF"));
  }
  
  memset(incomingCommand, 0, sizeof(incomingCommand));

  delay(100);
  
}//--(end main loop )---

/*-----( Declare User-written Functions )-----*/
void printTemperature(DeviceAddress deviceAddress)
{

float tempC = sensors.getTempC(deviceAddress);

   if (tempC == -127.00) 
   {
   Serial.print("Error getting temperature  ");
   } 
   else
   {  
     //Serial.print("C: ");
     //Serial.print(tempC);
     Serial.print(DallasTemperature::toFahrenheit(tempC));
     Serial.print("F ");
   }
}// End printTemperature
//*********( THE END )***********

