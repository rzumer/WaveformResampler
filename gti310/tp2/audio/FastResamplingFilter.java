package gti310.tp2.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FastResamplingFilter extends ResamplingFilter
{
	private byte[] lastFrameProcessed; // Represents the last frame of the last segment processed with the filter, used for interpolation.
	private double segmentOffset; // Used to balance the frame selection between segments.
	
	public FastResamplingFilter(int outSampleRate)
	{
		super(outSampleRate);
		
		segmentOffset = 0;
	}
	
	@Override
	public void setInputProperties(AudioProperties properties)
	{
		super.setInputProperties(properties);
		
		segmentOffset = 0;
	}

	@Override
	public byte[] process(byte[] input)
	{
		if(outProperties.BitsPerSample > 16)
		{
			throw new UnsupportedOperationException();
		}
		
		int outSampleRate = outProperties.SampleRate;
		int frameSize = properties.getFrameSize();
		double decimationRate = (double)properties.SampleRate / outSampleRate;
		
		ByteBuffer inputBuffer = ByteBuffer.wrap(input);
		inputBuffer.order(properties.ByteOrder);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		int frameCount = 0;
		while(inputBuffer.remaining() > frameSize)
		{
			double framePointer = (double)segmentOffset + (frameCount * decimationRate);
			double weight = MathHelper.Round(framePointer % 1, 6);
			
			int leftFrameNumber = (int) Math.round(framePointer - weight);
			int rightFrameNumber = leftFrameNumber + 1;
			
			// The while check is insufficient to ensure that both the new left frame and the right frame, if one is needed, are present in this segment.
			if(inputBuffer.limit() <= (weight > 0 ? rightFrameNumber : leftFrameNumber) * frameSize)
			{
				break;
			}
			
			// Read samples in the left and right frames.
			int[] leftSamples = leftFrameNumber < 0 ? 
					readFrameSamples(lastFrameProcessed) : readFrameSamples(inputBuffer, leftFrameNumber);
			int[] rightSamples = weight > 0 ? readFrameSamples(inputBuffer, rightFrameNumber) : null;

			// Perform linear interpolation for each sample.
			int[] interpolatedSamples = new int[leftSamples.length];
			
			for(int channel = 0; channel < leftSamples.length; channel++)
			{
				interpolatedSamples[channel] = weight > 0 ? MathHelper.InterpolateLinear(leftSamples[channel], rightSamples[channel], ((weight + 1) % 1)) : leftSamples[channel];
				
				try
				{
					// Write interpolated samples to the output stream.
					if(properties.BitsPerSample <= 8)
					{
						outputStream.write((byte) interpolatedSamples[channel]);
					}
					else if(properties.BitsPerSample <= 16)
					{
						outputStream.write(ByteHelper.GetShortBytes((short) interpolatedSamples[channel], properties.ByteOrder));
					}
					else if(properties.BitsPerSample <= 24)
					{
						outputStream.write(ByteHelper.GetNumberBytes(interpolatedSamples[channel], properties.ByteOrder, 3));
					}
					else
					{
						// Assuming 32-bit signed integers.
						outputStream.write(ByteHelper.GetIntBytes(interpolatedSamples[channel], properties.ByteOrder));
					}
				}
				catch (IOException e)
				{
					System.err.println("Error Writing Output Stream");
				}	
			}
			
			// Basic depth checking.
			if(frameCount++ > Math.round((double)input.length / (decimationRate * frameSize) - segmentOffset))
			{
				throw new IndexOutOfBoundsException();
			}
			
			// Place the buffer back to the first byte of the left frame.
			inputBuffer.position(Math.max(leftFrameNumber * frameSize, 0));
		}
		
		lastFrameProcessed = Arrays.copyOfRange(input, input.length - frameSize, input.length);
		segmentOffset -= ((input.length / (decimationRate * frameSize) - (frameCount))) * decimationRate;
		
		return outputStream.toByteArray();
	}
	
	// Reads the contents of a frame into an integer per channel value, used for processing.
	private int[] readFrameSamples(ByteBuffer buffer, int frameNumber)
	{
		buffer.position(frameNumber * properties.getFrameSize());
		
		int[] samples = new int[properties.NumChannels];
		
		for(int channel = 0; channel < properties.NumChannels; channel++)
		{
			if(properties.BitsPerSample <= 8)
			{
				samples[channel] = buffer.get() & 0xff;
			}
			else if(properties.BitsPerSample <= 16)
			{
				samples[channel] = buffer.getShort();
			}
			else if(properties.BitsPerSample <= 24)
			{
				byte[] sampleBytes = new byte[3];
				buffer.get(sampleBytes);
				
				samples[channel] = ByteHelper.GetIntFromBytes(sampleBytes, buffer.order());
			}
			else
			{
				// Assuming 32-bit signed integers.
				samples[channel] = buffer.getInt();
			}
		}
		
		return samples;
	}
	
	private int[] readFrameSamples(byte[] frame)
	{
		return readFrameSamples(ByteBuffer.wrap(frame), 0);
	}
}
