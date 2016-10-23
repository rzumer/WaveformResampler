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
		int lcm = MathExtensions.LeastCommonMultiple(properties.SampleRate, outSampleRate);
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
		for(int i = 0; i < stuffedInput.length; i+= properties.getFrameSize() * (padding + 1))
		{
			int j = i + (properties.getFrameSize() * (padding + 1));
			
			if(j >= stuffedInput.length)
			{
				j = i;
			}
			
			//byte[] frame = Arrays.copyOfRange(stuffedInput, i, i + properties.getFrameSize());
			//byte[] frame2 = Arrays.copyOfRange(stuffedInput, j, j + properties.getFrameSize()); not on last frame
			
			if(properties.NumChannels > 1)
			{
				// process each channel independently
			}
			
			if(properties.BitsPerSample > 8)
			{
				// process multibyte values
			}
			
			// 8-bit monaural processing
			short a = stuffedInput[i];
			short b = stuffedInput[j];
			
			for(int k = 0; k < padding; k++)
			{
				int index = i + properties.getFrameSize() + k - 1;
				if(index < stuffedInput.length)
				{
					stuffedInput[index] = (byte) MathExtensions.InterpolateLinear(a, b, (float) k / padding);
				}
			}
		}
		
		// TODO filter out samples with an amplitude above Nyquist (2 x output sample rate)
		
		// Decimation
		int decimationRate = lcm / outSampleRate;
		int outputSize = Math.round(input.length * (float)properties.SampleRate / outSampleRate);
		
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
		
		properties.SampleRate = outSampleRate;
		
		return downsampledInput;
	}
}

final class MathExtensions
{
	public static int LeastCommonMultiple(int a, int b)
	{
		return a / GreatestCommonDivisor(a, b) * b;
	}
	
	public static int GreatestCommonDivisor(int a, int b)
	{
		while(b != 0)
		{
			int temp = b;
			b = a % b;
			a = temp;
		}
		
		return a;
	}
	
	public static int InterpolateLinear(int a, int b, float weight)
	{
		if(weight < 0)
		{
			return a;
		}
		else if(weight > 1)
		{
			return b;
		}
		
		return Math.round(a + ((b - a) * weight));
	}
}
