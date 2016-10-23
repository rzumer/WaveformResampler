package gti310.tp2.audio;

import java.lang.UnsupportedOperationException;
import java.util.Arrays;

public class ResamplingFilter extends AudioFilter
{
	@Override
	public byte[] process(byte[] input)
	{
		return process(input, 8000); // Default output sample rate of 8000 Hz
	}
	
	public byte[] process(byte[] input, int outSampleRate)
	{
		/*if(outSampleRate == properties.SampleRate)
		{
			// No processing needed.
			return input;
		}
		
		else*/ if(outSampleRate > properties.SampleRate)
		{
			// Upsampling is not implemented.
			throw new UnsupportedOperationException();
		}
		
		// Simple processing
		// Use FIR interpolation to save CPU/Memory
		
		// Bit Stuffing
		int lcm = MathHelper.LeastCommonMultiple(properties.SampleRate, outSampleRate);
		int padding = ((lcm / properties.SampleRate) - 1) * properties.getFrameSize();
		
		// Stuffed input contains the initial bytes, plus the zeroes which are between each frame.
		byte[] stuffedInput = new byte[input.length + (input.length / properties.getFrameSize() * padding) - padding];
		int stuffedInputPointer = 0;
		
		for(int i = 0; i < input.length; i+= properties.getFrameSize())
		{
			byte[] frame = Arrays.copyOfRange(input, i, i + properties.getFrameSize());
			
			for(byte b : frame)
			{
				stuffedInput[stuffedInputPointer] = b;
				stuffedInputPointer++;
			}
			
			// Do not stuff after the last frame.
			if(i < input.length - properties.getFrameSize())
			{
				for(int j = 0; j < padding; j++)
				{
					stuffedInput[stuffedInputPointer] = 0;
					stuffedInputPointer++;
				}
			}
		}
		
		// Linear Interpolation
		for(int i = 0; i < stuffedInput.length; i += properties.getFrameSize() * (padding + 1))
		{
			int j = i + (properties.getFrameSize() * (padding + 1));
			
			// Retrieve frames to interpolate.
			// If the left frame is the last one, do not interpolate. 
			byte[] leftFrame = Arrays.copyOfRange(stuffedInput, i, i + properties.getFrameSize());
			byte[] rightFrame = j >= stuffedInput.length ? 
					leftFrame : Arrays.copyOfRange(stuffedInput, j, j + properties.getFrameSize());
			
			if(properties.NumChannels > 1)
			{
				// process each channel independently
			}
			
			if(properties.BitsPerSample > 8)
			{
				// process multibyte values
			}
			else
			{
				// 8-bit monaural processing
				int leftSample = GetByteAsUnsignedInt(stuffedInput[i]); // for 8-bit only
				int rightSample = 0;
				if(j < stuffedInput.length)
				{
					rightSample = GetByteAsUnsignedInt(stuffedInput[j]); // for 8-bit only
				}
				
				for(int k = 0; k < padding; k++)
				{
					int index = i + properties.getFrameSize() + k;
					if(index < stuffedInput.length)
					{
						int interpolated = MathHelper.InterpolateLinear(leftSample, rightSample, (float)k / padding);
						stuffedInput[index] = (byte)interpolated;
					}
				}
			}
		}
		
		// TODO filter out samples with an amplitude above Nyquist (2 x output sample rate)
		
		// Decimation
		int decimationRate = lcm / outSampleRate;
		int outputSize = Math.round(input.length / ((float)properties.SampleRate / outSampleRate));
		
		byte[] downsampledInput = new byte[outputSize];
		int decimatedInputPointer = 0;
		
		for(int i = 0; i < stuffedInput.length; i += decimationRate * properties.getFrameSize())
		{
			byte[] frame = Arrays.copyOfRange(stuffedInput, i, i + properties.getFrameSize());
			
			for(byte b : frame)
			{
				downsampledInput[decimatedInputPointer] = b;
				decimatedInputPointer++;
			}
		}
		
		outProperties.SampleRate = outSampleRate;
		
		return downsampledInput;
	}
	
	private static int GetByteAsUnsignedInt(byte b)
	{
		return b & 0xff;
	}
}
