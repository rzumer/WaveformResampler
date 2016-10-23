package gti310.tp2.audio;

import static org.junit.Assert.*;

import org.junit.Test;

public class MathTest {

	@Test
	public void test() {
		int lcm = MathHelper.LeastCommonMultiple(44100, 8000);
		assertEquals(lcm, 3528000);
		
		lcm = MathHelper.LeastCommonMultiple(44000, 8000);
		assertEquals(lcm, 88000);
		
		lcm = MathHelper.LeastCommonMultiple(48000, 44000);
		assertEquals(lcm, 528000);
	}

}
