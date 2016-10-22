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
	
	public byte[] process(byte[] input, int sampleRate)
	{
		if(sampleRate == properties.SampleRate)
		{
			// No processing needed.
			return input;
		}
		
		else if(sampleRate > properties.SampleRate)
		{
			// Upsampling is not implemented.
			throw new UnsupportedOperationException();
		}
		
		// Step 1: Interpolation
		int lcm = MathExtensions.LeastCommonMultiple(properties.SampleRate, sampleRate);
		int padding = (lcm / properties.SampleRate) - 1;
		
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
		
		return stuffedInput;
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
}
