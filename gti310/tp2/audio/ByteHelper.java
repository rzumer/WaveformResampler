package gti310.tp2.audio;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ByteHelper 
{
	public static int GetIntFromBytes(byte[] data, ByteOrder byteOrder)
	{
		if(data.length >= 2)
		{
			ByteBuffer dataBuffer = ByteBuffer.wrap(data);
			dataBuffer.order(byteOrder);
			
			if(data.length >= 3)
			{
				return dataBuffer.getInt();
			}
			
			return dataBuffer.getShort();
		}
		
		return 0;
	}
	
	public static byte[] GetIntBytes(int data, ByteOrder byteOrder)
	{
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
	    buffer.order(byteOrder);
	    buffer.putInt(data);
	    return buffer.array();
	}
	
	public static byte[] GetShortBytes(short data, ByteOrder byteOrder)
	{
		ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
	    buffer.order(byteOrder);
	    buffer.putShort(data);
	    return buffer.array();
	}
	
	public static byte[] GetASCIIBytes(String data, ByteOrder byteOrder)
	{
		try {
			return data.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
}
