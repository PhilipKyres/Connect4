package ca.mkp.connect4.common.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

/**
 *	Responsible for sending and receiving game move information.
 *	Packs and unpacks information in a single byte.
 * 
 * @author Philip Kyres
 */
public class C4PacketManager 
{
	/*
	 * Packet size is 1 byte
	 * 4 bits used for the message id (0 to 15)
	 * 3 bits used for the column index (0 to 7)
	 */
	public static final int PACKET_SIZE = 1;
	
	// Max number of messages is 16
	public static final byte GAME_STARTED = 0;
	public static final byte GAME_WON =  1;
	public static final byte GAME_LOST =  2;
	public static final byte CLIENT_CONNECTED = 3;
	public static final byte CLIENT_DISCONNECTED = 4;
	public static final byte CLIENT_LOST_CONNECTION = 5;
	public static final byte SERVER_LOST_CONNECTION = 6;
	public static final byte PLAYER_MOVE = 7;
	public static final byte AI_MOVE = 8;
	public static final byte INVALID_MOVE = 9;
	public static final byte GAME_TIE = 10;
	public static final byte GAME_ABORT = 11;
	
	private InputStream inputStream;
	private OutputStream outputStream;

	public C4PacketManager(InputStream inputStream, OutputStream outputStream) 
	{
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	/**
	 * Packs and sends a packet.
	 * @throws IOException
	 */
	public void sendPacket(byte column, byte msg) throws IOException 
	{
	    byte packet[] = new byte[PACKET_SIZE];
	    packet[0] = packByte(column, msg);
		outputStream.write(packet, 0, PACKET_SIZE);
	}

	/**
	 * Receives and unpacks a packet.
	 * @return byte array with 2 elements, the message id and column index
	 * @throws IOException
	 * @throws SocketException
	 */
	public byte[] receivePacket() throws IOException, SocketException 
	{
	    int totalBytesRcvd = 0;						
	    int bytesRcvd;
	    byte packet[] = new byte[PACKET_SIZE];
	    
	    while (totalBytesRcvd < PACKET_SIZE)
	    {
	    	bytesRcvd = inputStream.read(packet, totalBytesRcvd, PACKET_SIZE - totalBytesRcvd);
	    	totalBytesRcvd += bytesRcvd;
	    }
	    
	    return unpackByte(packet[0]);
	}
	
	/**
	 * Packs the message id and column index into one byte.
	 * @param column Column index
	 * @param msg Message id
	 * @return Packed byte
	 */
	private byte packByte(byte column, byte msg)
	{
		byte packed = 0;
		
		packed |= msg;
		packed |= (column << 4);
		return packed;
	}
	
	/**
	 * Unpacks the message id and column index from the packed byte into a byte array.
	 * @param packed Packed byte
	 * @return byte array with 2 elements, the header message and column index
	 */
	private byte[] unpackByte(byte packed)
	{
		byte unpacked[] = new byte[2];
		unpacked[0] = (byte)(packed & 0xf);
		unpacked[1] = (byte)(packed >> 4 & 0xf);
		return unpacked;
	}
}
