package audioresampler.audio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Provides some static methods for common math operations.
 * 
 * @author Raphaël Zumer <rzumer@gmail.com>
 */
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
	
	public static int InterpolateLinear(int a, int b, double distance)
	{
		if(distance <= 0)
		{
			return a;
		}
		else if(distance >= 1)
		{
			return b;
		}
		
		return (int) Math.round(a + ((b - a) * distance));
	}
	
	public static double Round(double value, int decimalPlaces)
	{
	    if (decimalPlaces < 0)
    	{
	    	throw new IllegalArgumentException();
    	}

	    BigDecimal bdValue = new BigDecimal(value);
	    bdValue = bdValue.setScale(decimalPlaces, RoundingMode.HALF_UP);
	    return bdValue.doubleValue();
	}
}
