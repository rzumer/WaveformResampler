package gti310.tp2.audio;

import java.util.Arrays;

import gti310.tp2.audio.AudioProperties.AudioFormat;

public class ResamplingFilter extends AudioFilter
{
	private byte[] lastFrameProcessed; // Represents the last frame of the last segment processed with the filter, used for interpolation.
	private int decimationOffset; // Represents the number of frames to skip initially during decimation.
	
	public ResamplingFilter(int outSampleRate)
	{
		super();
		
		outProperties.SampleRate = outSampleRate;
		lastFrameProcessed = null;
		decimationOffset = 0;
	}
	
	@Override
	public void setInputProperties(AudioProperties properties)
	{
		super.setInputProperties(properties);
		
		// Maintain the sample rate filter parameter even if the input changes.
		if(outProperties.SampleRate > 0)
		{
			int outSampleRate = outProperties.SampleRate;
			
			outProperties = properties.copy();
			outProperties.SampleRate = outSampleRate;
		}
		
		lastFrameProcessed = null;
		decimationOffset = 0;
	}

	@Override
	public byte[] process(byte[] input)
	{
		if(outProperties.SampleRate <= 0)
		{
			throw new IllegalArgumentException();	
		}
		
		if(properties.Format != AudioFormat.WAVE_PCM)
		{
			try
			{
				throw new UnsupportedFormatException();
			}
			catch (UnsupportedFormatException e)
			{
				System.err.println("Unsupported Format");
			}
		}
		
		if(outProperties.SampleRate == properties.SampleRate)
		{
			// No processing needed.
			return input;
		}
		
		/*else if(outProperties.SampleRate < properties.SampleRate)
		{
			// Use the fast algorithm with linear interpolation.
			try
			{
				return fastDownsample(input);
			}
			catch (IOException e) 
			{
				System.err.println("Error Writing Output Stream");
				
				// Fall back to the generic algorithm.
				return resample(input);
			}
		}
		
		return resample(input);*/
		
		// Note: it would be more efficient with linear interpolation to weigh samples directly using the input/output sample rate ratio.
		int outSampleRate = outProperties.SampleRate;
		int frameSize = properties.getFrameSize();
		int sampleRateLCM = MathHelper.LeastCommonMultiple(properties.SampleRate, outSampleRate);
		
		// Bit Stuffing
		int padding = ((sampleRateLCM / properties.SampleRate) - 1) * frameSize;
		
		// Stuffed input contains the initial bytes, plus zeroes between each frame as padding.
		// If this is not the first segment being processed, append extra padding at the start, which will be interpolated with the last frame.
		byte[] stuffedInput = 
				lastFrameProcessed == null ? 
				new byte[input.length + (input.length / frameSize * padding) - padding] : 
				new byte[input.length + (input.length / frameSize * padding)];
		
		int stuffedInputPointer = lastFrameProcessed == null ? 0 : padding;
		
		for(int i = 0; i < input.length; i+= frameSize)
		{
			byte[] frame = Arrays.copyOfRange(input, i, i + frameSize);
			
			for(byte b : frame)
			{
				stuffedInput[stuffedInputPointer] = b;
				stuffedInputPointer++;
			}
			
			stuffedInputPointer += padding;
		}
		
		// Linear Interpolation
		for(int i = 0 - (lastFrameProcessed == null ? 0 : frameSize); i < stuffedInput.length; i += frameSize + padding)
		{
			int j = i + frameSize + padding;
			
			// Retrieve frames to interpolate.
			byte[] leftFrame = i >= 0 ? Arrays.copyOfRange(stuffedInput, i, i + frameSize) : lastFrameProcessed;
			byte[] rightFrame = j >= stuffedInput.length ? 
					null : Arrays.copyOfRange(stuffedInput, j, j + frameSize);

			for(int channel = 0; channel < properties.NumChannels; channel++)
			{
				int leftSample = ByteHelper.GetIntFromBytes(
						Arrays.copyOfRange(leftFrame, channel * properties.getChannelSize(), (channel + 1) * properties.getChannelSize()), 
						properties.ByteOrder, properties.BitsPerSample > 8);
				int rightSample = rightFrame == null ? ByteHelper.GetZeroByte(properties.BitsPerSample > 8) : ByteHelper.GetIntFromBytes(
						Arrays.copyOfRange(rightFrame, channel * properties.getChannelSize(), (channel + 1) * properties.getChannelSize()),
						properties.ByteOrder, properties.BitsPerSample > 8);
				
				for(int k = 0; k < (padding / frameSize); k++)
				{
					int index = i + (frameSize * (k + 1)) + (channel * properties.getChannelSize());
					
					if(index + properties.getChannelSize() <= stuffedInput.length)
					{
						int interpolatedSample = MathHelper.InterpolateLinear(leftSample, rightSample, 
								(double)(k + 1) / ((padding / frameSize) + 1));
						
						byte[] interpolatedBytes = properties.BitsPerSample == 8 ?
								new byte[] { (byte)interpolatedSample } : properties.BitsPerSample == 16 ? 
								ByteHelper.GetShortBytes((short)interpolatedSample, properties.ByteOrder) :
								ByteHelper.GetIntBytes(interpolatedSample, properties.ByteOrder);
						
						for(int l = 0; l < interpolatedBytes.length; l++)
						{
							stuffedInput[index + l] = interpolatedBytes[l]; 
						}
					}
				}
			}
		}
		
		// Decimation
		int decimationRate = sampleRateLCM / outSampleRate;
		int outputSize = (int) Math.round(((double)stuffedInput.length / decimationRate));
		
		// Add extra bytes if the end of the file is uneven.
		while(outputSize % frameSize != 0)
		{
			outputSize++;
		}
		
		byte[] downsampledInput = new byte[outputSize];
		int decimatedInputPointer = 0;
		
		for(int i = decimationOffset * frameSize; i < stuffedInput.length; i += decimationRate * frameSize)
		{
			byte[] frame = Arrays.copyOfRange(stuffedInput, i, i + frameSize);
			
			for(byte b : frame)
			{
					downsampledInput[decimatedInputPointer] = b;
					decimatedInputPointer++;
			}
			
			decimationOffset = Math.abs(stuffedInput.length - (decimationRate * frameSize) - i) / frameSize;
		}
		
		// Fill in any remaining bytes with silence.
		while(decimatedInputPointer < outputSize)
		{
			downsampledInput[decimatedInputPointer] = ByteHelper.GetZeroByte(properties.BitsPerSample > 8); 
			decimatedInputPointer++;
		}
		
		lastFrameProcessed = Arrays.copyOfRange(input, input.length - frameSize, input.length);
		
		return downsampledInput;
	}
}
