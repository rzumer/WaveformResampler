package gti310.tp2.audio;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ByteHelper 
{
	public static int GetIntFromBytes(byte[] data, ByteOrder byteOrder, boolean signed)
	{
		if(data == null || data.length == 0)
		{
			return 0;
		}
		
		if(data.length == 1)
		{
			return signed ? data[0] : (int)data[0] & 0xff;
		}
		
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		dataBuffer.order(byteOrder);
		
		if(data.length < 4)
		{
			return signed ? dataBuffer.getShort() : ((int)dataBuffer.getShort() & 0xffff);			
		}
		
		return signed ? dataBuffer.getInt() : (dataBuffer.getInt() & 0xffff);
	}
	
	public static int GetIntFromBytes(byte[] data, ByteOrder byteOrder)
	{
		return GetIntFromBytes(data, byteOrder, true);
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
