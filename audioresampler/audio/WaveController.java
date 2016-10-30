package audioresampler.audio;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import audioresampler.audio.AudioProperties.AudioFormat;
import audioresampler.io.FileSink;
import audioresampler.io.FileSource;

public class WaveController extends AudioController
{
	public WaveController(FileSource source, FileSink sink) throws HeaderFormatException, UnsupportedFormatException
	{
		super(source, sink);
	}

	@Override
	protected void initializeProperties()
	{
		// Reset properties to default values.
		properties = new AudioProperties();

		try
		{
			// Attempt to retrieve the header from the start of the file to the data chunk ID.
			byte[] header = popHeader();

			// Assign header values to properties and validate it.
			if(!parseHeader(header))
			{
				throw new HeaderFormatException();
			}
		}
		catch (IOException e)
		{
			System.err.println("Header Read Error");
		}
		catch (HeaderFormatException e)
		{
			System.err.println("Invalid WAVE Header");
		}
		catch (UnsupportedFormatException e)
		{
			System.err.println("Unsupported Format");
		}
	}

	// O(n)
	private byte[] popHeader() throws IOException, HeaderFormatException
	{
		// Main header buffer for iterating through fields
		ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
		// Secondary buffers for parsing values
		byte[] subChunkIDField;
		byte[] subChunkSizeField;
		int subChunkSize = 0;

		// Parse the RIFF chunk
		subChunkIDField = fileSource.pop(4);
		headerStream.write(subChunkIDField);
		if(!new String(subChunkIDField, "US-ASCII").equals("RIFF"))
		{
			throw new HeaderFormatException();
		}

		// Write the rest of the RIFF chunk
		headerStream.write(fileSource.pop(8));

		// Parse subchunks until the data subchunk is reached
		do
		{
			headerStream.write(fileSource.pop(subChunkSize));

			if(fileSource.getBytesRemaining() < 4)
			{
				throw new HeaderFormatException();
			}

			// Subchunk ID
			subChunkIDField = fileSource.pop(4);
			headerStream.write(subChunkIDField);

			// Subchunk Size
			subChunkSizeField = fileSource.pop(4);
			subChunkSize = (subChunkSizeField[0] & 0xFF) |
					(subChunkSizeField[1] & 0xFF) << 8 |
					(subChunkSizeField[2] & 0xFF) << 16 |
					(subChunkSizeField[3] & 0xFF) << 24;
			headerStream.write(subChunkSizeField);
		}
		while(!new String(subChunkIDField, "US-ASCII").equals("data"));

		return headerStream.toByteArray();
	}

	// O(n)
	private boolean parseHeader(byte[] header) throws UnsupportedEncodingException, UnsupportedFormatException
	{
		// Main header buffer for iterating through fields
		ByteBuffer headerBuffer = ByteBuffer.wrap(header);

		// Secondary buffers for parsing multibyte values
		byte[] field = new byte[4];
		byte[] longField = new byte[16];
		byte[] shortField = new byte[2];

		// The first field of each chunk is read in a loop, in order to account for extra data or offsets in the file header.

		// RIFF Chunk ID
		headerBuffer.get(field);

		if(!new String(field, "US-ASCII").equals("RIFF"))
		{
			// RIFX is not yet supported.
			return false;
		}
		else
		{
			properties.ByteOrder = ByteOrder.LITTLE_ENDIAN;
		}

		// RIFF Chunk Size
		headerBuffer.get(field);

		// WAVE ID
		headerBuffer.get(field);

		if(!new String(field, "US-ASCII").equals("WAVE"))
		{
			return false;
		}

		// FMT Chunk ID
		do
		{
			if(!headerBuffer.hasRemaining())
			{
				return false;
			}

			headerBuffer.get(field);
		}
		while(!new String(field, "US-ASCII").equals("fmt "));

		// FMT Chunk Size
		headerBuffer.get(field);
		int fmtChunkSize = ByteHelper.GetIntFromBytes(field, properties.ByteOrder);

		// Only PCM (16) is supported.
		switch(fmtChunkSize)
		{
			case 16:
				break;
			case 18:
				throw new UnsupportedFormatException();
			case 40:
				throw new UnsupportedFormatException();
			default:
				return false;
		}

		// Audio Format Code
		headerBuffer.get(shortField);
		int audioFormat = ByteHelper.GetIntFromBytes(shortField, properties.ByteOrder);

		if(audioFormat != 1)
		{
			// Non-PCM audio is not yet supported.
			return false;
		}
		else
		{
			properties.Format = AudioFormat.WAVE_PCM;
		}

		// Number of Channels
		headerBuffer.get(shortField);
		properties.NumChannels = (short) ByteHelper.GetIntFromBytes(shortField, properties.ByteOrder);

		// Sampling Rate
		headerBuffer.get(field);
		properties.SampleRate = ByteHelper.GetIntFromBytes(field, properties.ByteOrder);

		// Data Rate
		headerBuffer.get(field);

		// Block Size
		headerBuffer.get(shortField);

		// Bits Per Sample
		headerBuffer.get(shortField);
		properties.BitsPerSample = (short) ByteHelper.GetIntFromBytes(shortField, properties.ByteOrder);

		if(fmtChunkSize >= 18)
		{
			// cbSize (Extension Size)
			headerBuffer.get(shortField);

			if(fmtChunkSize >= 40)
			{
				// Valid Bits Per Sample
				headerBuffer.get(shortField);

				// Speaker Position Mask
				headerBuffer.get(field);

				// SubFormat
				headerBuffer.get(longField);
			}
		}

		// Data Chunk ID
		do
		{
			if(!headerBuffer.hasRemaining())
			{
				return false;
			}

			headerBuffer.get(field);
		}
		while(!new String(field, "US-ASCII").equals("data"));

		// Data Chunk Size
		headerBuffer.get(field);
		properties.DataSize = ByteHelper.GetIntFromBytes(field, properties.ByteOrder);

		return true;
	}

	@Override
	public void applyFilter(AudioFilter filter)
	{
		if(properties.Format != AudioFormat.WAVE_PCM)
		{
			System.err.println("Cannot Apply Filter");
			return;
		}

		filter.setInputProperties(properties);

		System.out.print("Applying " + filter.getClass().getSimpleName());

		// Apply the filter by 1 second segments.
		byte[] bytesPopped = null;

		try
		{
			for(int bytesWritten = 0; bytesWritten < properties.DataSize; bytesWritten += bytesPopped.length)
			{
				bytesPopped = fileSource.pop(Math.min(fileSource.getBytesRemaining(), properties.getFrameSize() * properties.SampleRate));

				fileSink.push(filter.process(bytesPopped));

				System.out.print(".");
			}

			// Process one last time with no data to receive the final remaining samples when upsampling.
			fileSink.push(filter.process(new byte[0]));
		}
		catch(Exception e)
		{
			System.err.println("\nFilter Processing Error: " + e);
			e.printStackTrace();
		}

		System.out.println();

		// Update properties with the filter's output.
		properties = filter.getOutputProperties();
	}

	// O(n)
	@Override
	public void saveToFile(String outputFilePath)
	{
		// Ensure that the file source and the file sink are closed before saving.
		close();

		byte[] header = GenerateFileSinkHeader();

		try
		{
			FileInputStream fis = new FileInputStream(fileSink.getLocation());
			FileSink fs = new FileSink(outputFilePath);

			fs.push(header);

			while(fis.available() > 0)
			{
				// Write in 1 second segments.
				byte[] buffer = new byte[properties.SampleRate];
				int bytesRead = fis.read(buffer);

				fs.push(Arrays.copyOfRange(buffer, 0, bytesRead));
			}

			// Add padding byte if the data length is odd.
			if((fs.getBytesWritten() & 1) != 0)
			{
				fs.push(new byte[] { 0 });
			}

			fis.close();
			fs.close();
		}
		catch (IOException e)
		{
			System.err.println("File Save I/O Error: " + e);
		}
	}

	// O(1)
	private byte[] GenerateFileSinkHeader()
	{
		byte[] header = new byte[44];
		int currentByte = 0;

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
		int chunkSize = 36 + properties.DataSize;
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
		int subchunk2Size = properties.DataSize;
		byte[] subchunk2SizeBytes = ByteHelper.GetIntBytes(subchunk2Size, ByteOrder.LITTLE_ENDIAN);
		for(byte b : subchunk2SizeBytes)
		{
			header[currentByte] = b;
			currentByte++;
		}

		return header;
	}
}
