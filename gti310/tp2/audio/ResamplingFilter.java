package gti310.tp2.audio;

public class ResamplingFilter extends AudioFilter
{
	@Override
	public byte[] process(byte[] input)
	{
		return process(input, 8000); // Default output sample rate of 8000 Hz
	}
	
	public byte[] process(byte[] input, int sampleRate)
	{
		return input;
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
