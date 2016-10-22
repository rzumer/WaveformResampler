package gti310.tp2.audio;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import gti310.tp2.io.FileSink;
import gti310.tp2.io.FileSource;

public class WaveController extends AudioController
{

	public WaveController(FileSource source, FileSink sink) throws HeaderFormatException, UnsupportedFormatException
	{
		super(source, sink);
	}
	
	@Override
	protected void initializeProperties() throws HeaderFormatException, UnsupportedFormatException
	{
		byte[] header = fileSource.pop(44);
		
		try
		{
			// Check that the file has a valid PCM WAVE header
			String format = new String(Arrays.copyOfRange(header, 8, 12), "US-ASCII");
			
			if(!format.equals("WAVE") || 
					GetIntFromBytes(Arrays.copyOfRange(header, 20, 22), 
					ByteOrder.LITTLE_ENDIAN) != 1)
			{
				throw new HeaderFormatException();
			}
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		properties.NumChannels = GetIntFromBytes(Arrays.copyOfRange(header, 22, 24), ByteOrder.LITTLE_ENDIAN);
		properties.SampleRate = GetIntFromBytes(Arrays.copyOfRange(header, 24, 28), ByteOrder.LITTLE_ENDIAN);
		properties.BitsPerSample = GetIntFromBytes(Arrays.copyOfRange(header, 34, 36), ByteOrder.LITTLE_ENDIAN);
		
		// Check for the proper input properties
		if(properties.SampleRate != 44100 || 
				(properties.BitsPerSample != 8 && properties.BitsPerSample != 16))
		{
			throw new UnsupportedFormatException();
		}
		
		fileSink.push(header);
	}

	@Override
	public void applyFilter(AudioFilter filter)
	{
		filter.setProperties(properties);
		
		byte[] bytesPopped = null;
		
		while(fileSource.bytesRemaining() > 0)
		{
			bytesPopped = fileSource.pop(Math.min(fileSource.bytesRemaining(), properties.getFrameSize()));
			
			byte[] bytesToPush = filter.process(bytesPopped);
			fileSink.push(bytesToPush);
		}
	}
	
	private static int GetIntFromBytes(byte[] data, ByteOrder byteOrder)
	{
		if(data.length >= 2)
		{
			ByteBuffer dataBuffer = ByteBuffer.wrap(data);
			dataBuffer.order(byteOrder);
			
			if(data.length >= 4)
			{
				return dataBuffer.getInt();
			}
			
			return dataBuffer.getShort();
		}
		
		return 0;
	}
}
