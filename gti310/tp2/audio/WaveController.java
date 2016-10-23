package gti310.tp2.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import gti310.tp2.io.FileSink;
import gti310.tp2.io.FileSource;

public class WaveController extends AudioController
{
	private final static int HeaderLength = 44;

	public WaveController(FileSource source, FileSink sink) throws HeaderFormatException, UnsupportedFormatException
	{
		super(source, sink);
	}
	
	@Override
	protected void initializeProperties() throws HeaderFormatException, UnsupportedFormatException
	{
		byte[] header = fileSource.pop(HeaderLength);
		
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
		
		properties.NumChannels = (short) GetIntFromBytes(Arrays.copyOfRange(header, 22, 24), ByteOrder.LITTLE_ENDIAN);
		properties.SampleRate = GetIntFromBytes(Arrays.copyOfRange(header, 24, 28), ByteOrder.LITTLE_ENDIAN);
		properties.BitsPerSample = (short) GetIntFromBytes(Arrays.copyOfRange(header, 34, 36), ByteOrder.LITTLE_ENDIAN);
		
		// Check for the proper input properties
		if(properties.SampleRate != 44100 || 
				(properties.BitsPerSample != 8 && properties.BitsPerSample != 16))
		{
			throw new UnsupportedFormatException();
		}
	}

	@Override
	public void applyFilter(AudioFilter filter)
	{
		filter.setProperties(properties);
		
		byte[] bytesPopped = null;
		
		while(fileSource.bytesRemaining() > 0)
		{
			bytesPopped = fileSource.pop(Math.min(fileSource.bytesRemaining(), properties.getFrameSize() * properties.SampleRate));
			
			byte[] bytesToPush = filter.process(bytesPopped);
			fileSink.push(bytesToPush);
		}
		
		// Update properties if they were changed while applying the filter.
		properties = filter.getProperties();
	}
	
	@Override
	public void saveToFile(String outputFilePath)
	{
		File outputFile = new File(outputFilePath);
		
		byte[] header = GenerateFileSinkHeader();
			
		try {
			RandomAccessFile raf = new RandomAccessFile(fileSink.getLocation(), "r");
			FileOutputStream fos = new FileOutputStream(outputFile);
			
			fos.write(header);
			
			for(int i = 0; i < raf.length(); i++)
			{
				fos.write(raf.readByte());
			}
			
			raf.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int GetIntFromBytes(byte[] data, ByteOrder byteOrder)
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
	
	private static byte[] GetIntBytes(int data, ByteOrder byteOrder)
	{
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
	    buffer.order(byteOrder);
	    buffer.putInt(data);
	    return buffer.array();
	}
	
	private static byte[] GetShortBytes(short data, ByteOrder byteOrder)
	{
		ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
	    buffer.order(byteOrder);
	    buffer.putShort(data);
	    return buffer.array();
	}
	
	private static byte[] GetASCIIBytes(String data, ByteOrder byteOrder)
	{
		try {
			return data.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	private byte[] GenerateFileSinkHeader()
	{
		byte[] header = new byte[HeaderLength];
		int currentByte = 0;
		int dataSize = (int) new File(fileSink.getLocation()).length();
		
		// ChunkID
		String id = "RIFF";
		byte[] idBytes = GetASCIIBytes(id, ByteOrder.BIG_ENDIAN);
		
		for(byte b : idBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// ChunkSize
		currentByte = 4;
		int chunkSize = 36 + dataSize;
		byte[] chunkSizeBytes = GetIntBytes(chunkSize, ByteOrder.LITTLE_ENDIAN);
		for(byte b : chunkSizeBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Format
		currentByte = 8;
		String format = "WAVE";
		byte[] formatBytes = GetASCIIBytes(format, ByteOrder.BIG_ENDIAN);
		for(byte b : formatBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk1ID
		currentByte = 12;
		String subchunk1ID = "fmt ";
		byte[] subchunk1IDBytes = GetASCIIBytes(subchunk1ID, ByteOrder.BIG_ENDIAN);
		for(byte b : subchunk1IDBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk1Size
		currentByte = 16;
		int subChunk1Size = 16;
		byte[] subchunk1SizeBytes = GetIntBytes(subChunk1Size, ByteOrder.LITTLE_ENDIAN);
		for(byte b : subchunk1SizeBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// AudioFormat
		currentByte = 20;
		short audioFormat = 1;
		byte[] audioFormatBytes = GetShortBytes(audioFormat, ByteOrder.LITTLE_ENDIAN);
		for(byte b : audioFormatBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// NumChannels
		currentByte = 22;
		short numChannels = properties.NumChannels;
		byte[] numChannelsBytes = GetShortBytes(numChannels, ByteOrder.LITTLE_ENDIAN);
		for(byte b : numChannelsBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// SampleRate
		currentByte = 24;
		int sampleRate = properties.SampleRate;
		byte[] sampleRateBytes = GetIntBytes(sampleRate, ByteOrder.LITTLE_ENDIAN);
		for(byte b : sampleRateBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// ByteRate
		currentByte = 28;
		short bitsPerSample = properties.BitsPerSample;
		int byteRate = sampleRate * numChannels * (bitsPerSample / 8);
		byte[] byteRateBytes = GetIntBytes(byteRate, ByteOrder.LITTLE_ENDIAN);
		for(byte b : byteRateBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// BlockAlign
		currentByte = 32;
		short blockAlign = (short) (numChannels * (bitsPerSample / 8));
		byte[] blockAlignBytes = GetShortBytes(blockAlign, ByteOrder.LITTLE_ENDIAN);
		for(byte b : blockAlignBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// BitsPerSample
		currentByte = 34;
		byte[] bitsPerSampleBytes = GetShortBytes(bitsPerSample, ByteOrder.LITTLE_ENDIAN);
		for(byte b : bitsPerSampleBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk2ID
		currentByte = 36;
		String subchunk2ID = "data";
		byte[] subchunk2IDBytes = GetASCIIBytes(subchunk2ID, ByteOrder.BIG_ENDIAN);
		for(byte b : subchunk2IDBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk2Size
		currentByte = 40;
		int subchunk2Size = dataSize;
		byte[] subchunk2SizeBytes = GetIntBytes(subchunk2Size, ByteOrder.LITTLE_ENDIAN);
		for(byte b : subchunk2SizeBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		return header;
	}
}
