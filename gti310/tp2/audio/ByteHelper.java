package gti310.tp2.audio;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ByteHelper 
{
	public static int GetIntFromBytes(byte[] data, ByteOrder byteOrder)
	{
		if(data == null || data.length == 0)
		{
			return 0;
		}
		
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		dataBuffer.order(byteOrder);
		
		switch(data.length)
		{
			case 1:
				return dataBuffer.get();
			case 2:
				return dataBuffer.getShort();
			case 3:
				byte[] buf = new byte[3];
				dataBuffer.get(buf);
				
				if(byteOrder == ByteOrder.LITTLE_ENDIAN)
				{
					return buf[2] << 16 | buf[1] << 8 | buf[0];					
				}
				
				return buf[0] << 16 | buf[1] << 8 | buf[2];
			case 4:
				return dataBuffer.getInt();
			default:
				throw new IllegalArgumentException();	
		}
	}
	
	public static int GetUnsignedIntFromByte(byte data)
	{
		return (int)data & 0xff;
	}
	
	public static byte[] GetNumberBytes(int data, ByteOrder byteOrder, int byteCount)
	{		
		byte[] dataBytes = new byte[byteCount];
		
		for(int i = 0; i < byteCount; i++)
		{
			dataBytes[i] = (byte) (data >> (i * 8));
		}
		
		return dataBytes;
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
	
	public static byte GetZeroByte(boolean signed)
	{
		return (byte) (signed ? 0 : 0 & 0xff);
	}
}
