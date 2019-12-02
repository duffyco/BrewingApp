package com.redbear.simplecontrols;

import java.text.ParseException;

import java.util.ArrayList;
import java.lang.Byte;
import java.util.Arrays;

/**
 * Created by lidia on 7/22/2015.
 */
public class BLEPacket {

    ArrayList<Byte> packetData = new ArrayList<Byte>();

    public static final byte HEADER = (byte) 0x0F;
    public static final byte TAIL = (byte) 0x0E;

    public static final byte TEMP = (byte) 0x01;
    public static final byte SET_PUMP = (byte) 0x02;
    public static final byte OVERRIDE = (byte) 0x03;

    public static final byte READ_TEMP = (byte) 0x0A;
    public static final byte READ_PUMP = (byte) 0x0B;

    public byte[] getPacket()
    {
        packetData.add( TAIL );
        byte[] packet = new byte[packetData.size()];

        for( int i=0; i < packet.length; i++ )
            packet[i] = packetData.get( i );

        return packet;
    }

    public byte getType() {
        return packetData.get( 1 );
    }

    public byte[] getData()
    {
        byte[] data = getPacket();
        return Arrays.copyOfRange( data, 2, data.length - 2 );
    }

    public void setData( byte[] inData )
    {
        for( byte b : inData )
            packetData.add( b );
    }

    public BLEPacket(byte type)
    {
        packetData.add( HEADER );
        packetData.add( type );
    }

    public BLEPacket( byte[] toParse ) throws ParseException
    {
        if( toParse[0] != HEADER )
            throw new ParseException("Invalid Packet", 0);

        boolean packetValid = false;
        for( int i=0; i < toParse.length; i++ ) {
            packetData.add( toParse[i] );

            if( toParse[i] == TAIL ) {
                packetValid = true;
                break;
            }
        }

        if( !packetValid )
            throw new ParseException( "No Tail on Packet", 1 );
    }
}
