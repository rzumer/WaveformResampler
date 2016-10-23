package gti310.tp2.audio;

final class MathHelper
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
		if(weight <= 0)
		{
			return a;
		}
		else if(weight >= 1)
		{
			return b;
		}
		
		return Math.round(a + ((float)(b - a) * weight));
	}
}
