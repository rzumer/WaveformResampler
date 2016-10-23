package gti310.tp2.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
		// TODO may have extra metadata, search for subchunk 2 instead ("data")
		byte[] header = fileSource.pop(44);
		
		try
		{
			// Check that the file has a valid PCM WAVE header
			String format = new String(Arrays.copyOfRange(header, 8, 12), "US-ASCII");
			
			if(!format.equals("WAVE") || 
					ByteHelper.GetIntFromBytes(Arrays.copyOfRange(header, 20, 22), 
					ByteOrder.LITTLE_ENDIAN) != 1)
			{
				throw new HeaderFormatException();
			}
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		properties.NumChannels = (short) ByteHelper.GetIntFromBytes(Arrays.copyOfRange(header, 22, 24), ByteOrder.LITTLE_ENDIAN);
		properties.SampleRate = ByteHelper.GetIntFromBytes(Arrays.copyOfRange(header, 24, 28), ByteOrder.LITTLE_ENDIAN);
		properties.BitsPerSample = (short) ByteHelper.GetIntFromBytes(Arrays.copyOfRange(header, 34, 36), ByteOrder.LITTLE_ENDIAN);
		
		// Check for the proper input properties
		if(properties.SampleRate != 44100 || 
				(properties.BitsPerSample != 8 && properties.BitsPerSample != 16))
		{
			//throw new UnsupportedFormatException();
		}
	}

	@Override
	public void applyFilter(AudioFilter filter)
	{
		filter.setInputProperties(properties);
		
		byte[] bytesPopped = null;
		
		while(fileSource.bytesRemaining() > 0)
		{
			bytesPopped = fileSource.pop(Math.min(fileSource.bytesRemaining(), properties.getFrameSize() * properties.SampleRate));
			
			byte[] bytesToPush = filter.process(bytesPopped);
			fileSink.push(bytesToPush);
		}
		
		// Update properties if they were changed while applying the filter.
		properties = filter.getOutputProperties();
	}
	
	@Override
	public void saveToFile(String outputFilePath)
	{
		// Ensure that the file source and the file sink are closed before saving.
		close();
		
		byte[] header = GenerateFileSinkHeader();
			
		try {
			FileInputStream fis = new FileInputStream(fileSink.getLocation());
			FileSink fs = new FileSink(outputFilePath);
			
			fs.push(header);
			
			// TODO write faster
			while(fis.available() > 0)
			{
				byte[] buffer = new byte[10000];
				int bytesRead = fis.read(buffer);
				
				fs.push(Arrays.copyOfRange(buffer, 0, bytesRead));
			}
			
			fis.close();
			fs.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private byte[] GenerateFileSinkHeader()
	{
		byte[] header = new byte[44];
		int currentByte = 0;
		int dataSize = (int) new File(fileSink.getLocation()).length();
		
		// ChunkID
		String id = "RIFF";
		byte[] idBytes = ByteHelper.GetASCIIBytes(id, ByteOrder.BIG_ENDIAN);
		
		for(byte b : idBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// ChunkSize
		currentByte = 4;
		int chunkSize = 36 + dataSize;
		byte[] chunkSizeBytes = ByteHelper.GetIntBytes(chunkSize, ByteOrder.LITTLE_ENDIAN);
		for(byte b : chunkSizeBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Format
		currentByte = 8;
		String format = "WAVE";
		byte[] formatBytes = ByteHelper.GetASCIIBytes(format, ByteOrder.BIG_ENDIAN);
		for(byte b : formatBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk1ID
		currentByte = 12;
		String subchunk1ID = "fmt ";
		byte[] subchunk1IDBytes = ByteHelper.GetASCIIBytes(subchunk1ID, ByteOrder.BIG_ENDIAN);
		for(byte b : subchunk1IDBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk1Size
		currentByte = 16;
		int subChunk1Size = 16;
		byte[] subchunk1SizeBytes = ByteHelper.GetIntBytes(subChunk1Size, ByteOrder.LITTLE_ENDIAN);
		for(byte b : subchunk1SizeBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// AudioFormat
		currentByte = 20;
		short audioFormat = 1;
		byte[] audioFormatBytes = ByteHelper.GetShortBytes(audioFormat, ByteOrder.LITTLE_ENDIAN);
		for(byte b : audioFormatBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// NumChannels
		currentByte = 22;
		short numChannels = properties.NumChannels;
		byte[] numChannelsBytes = ByteHelper.GetShortBytes(numChannels, ByteOrder.LITTLE_ENDIAN);
		for(byte b : numChannelsBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// SampleRate
		currentByte = 24;
		int sampleRate = properties.SampleRate;
		byte[] sampleRateBytes = ByteHelper.GetIntBytes(sampleRate, ByteOrder.LITTLE_ENDIAN);
		for(byte b : sampleRateBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// ByteRate
		currentByte = 28;
		short bitsPerSample = properties.BitsPerSample;
		int byteRate = sampleRate * numChannels * (bitsPerSample / 8);
		byte[] byteRateBytes = ByteHelper.GetIntBytes(byteRate, ByteOrder.LITTLE_ENDIAN);
		for(byte b : byteRateBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// BlockAlign
		currentByte = 32;
		short blockAlign = (short) (numChannels * (bitsPerSample / 8));
		byte[] blockAlignBytes = ByteHelper.GetShortBytes(blockAlign, ByteOrder.LITTLE_ENDIAN);
		for(byte b : blockAlignBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// BitsPerSample
		currentByte = 34;
		byte[] bitsPerSampleBytes = ByteHelper.GetShortBytes(bitsPerSample, ByteOrder.LITTLE_ENDIAN);
		for(byte b : bitsPerSampleBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk2ID
		currentByte = 36;
		String subchunk2ID = "data";
		byte[] subchunk2IDBytes = ByteHelper.GetASCIIBytes(subchunk2ID, ByteOrder.BIG_ENDIAN);
		for(byte b : subchunk2IDBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		// Subchunk2Size
		currentByte = 40;
		int subchunk2Size = dataSize;
		byte[] subchunk2SizeBytes = ByteHelper.GetIntBytes(subchunk2Size, ByteOrder.LITTLE_ENDIAN);
		for(byte b : subchunk2SizeBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}
		
		return header;
	}
}
