package gti310.tp2.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FastResamplingFilter extends ResamplingFilter
{
	private float segmentOffset;
	
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
		int outSampleRate = outProperties.SampleRate;
		int frameSize = properties.getFrameSize();
		float decimationRate = (float)properties.SampleRate / outSampleRate;
		
		ByteBuffer inputBuffer = ByteBuffer.wrap(input);
		inputBuffer.order(properties.ByteOrder);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		int frameCount = 0;
		while(inputBuffer.remaining() >= (2 * decimationRate * frameSize + 1))
		{
			//System.out.println(inputBuffer.remaining());
			float framePointer = segmentOffset + (frameCount * decimationRate);
			float weight = framePointer % 1;
			int leftFrameNumber = Math.round(framePointer - weight);
			int rightFrameNumber = leftFrameNumber + 1;
			
			// Write the left frame into the output stream if it does not need to be interpolated.
			if(Math.round(weight) == 0)
			{
				byte[] leftFrame = new byte[frameSize];
				inputBuffer.get(leftFrame);
				
				try {
					outputStream.write(leftFrame);
				} catch (IOException e) {
					System.err.println("Error Writing Output Stream");
				}
			}
			else
			{
				// Read samples in the left and right frames.
				int[] leftSamples = leftFrameNumber < 0 ? 
						readFrameSamples(lastFrameProcessed) : readFrameSamples(inputBuffer, leftFrameNumber);
				int[] rightSamples = readFrameSamples(inputBuffer, rightFrameNumber);
	
				// Perform linear interpolation for each sample.
				int[] interpolatedSamples = new int[leftSamples.length];
				
				for(int channel = 0; channel < leftSamples.length; channel++)
				{
					interpolatedSamples[channel] = MathHelper.InterpolateLinear(leftSamples[channel], rightSamples[channel], weight);
					
					// Write interpolated samples to the output stream.
					if(properties.BitsPerSample <= 8)
					{
						outputStream.write((byte) interpolatedSamples[channel]);
					}
					else if(properties.BitsPerSample <= 16)
					{
						try {
							outputStream.write(ByteHelper.GetShortBytes((short) interpolatedSamples[channel], properties.ByteOrder));
						} catch (IOException e) {
							System.err.println("Error Writing Output Stream");
						}					
					}
					else
					{
						// Assuming data is in 32-bit signed integers.
						try {
							outputStream.write(ByteHelper.GetIntBytes(interpolatedSamples[channel], properties.ByteOrder));
						} catch (IOException e) {
							System.err.println("Error Writing Output Stream");
						}
					}
				}
			}
			
			frameCount++;
			
			if(frameCount > Math.round((float)input.length / (decimationRate * frameSize)))
			{
				throw new IndexOutOfBoundsException();
			}
			
			// Place the buffer back to the first byte of the left frame.
			inputBuffer.position(leftFrameNumber * properties.getFrameSize());
		}
		
		lastFrameProcessed = Arrays.copyOfRange(input, input.length - frameSize, input.length);
		segmentOffset = ((input.length - (decimationRate * frameSize) - inputBuffer.position()) / frameSize) * -1;
		
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
			else
			{
				// Assuming data is in 32-bit signed integers.
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
