package audioresampler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import audioresampler.audio.AudioController;
import audioresampler.audio.HeaderFormatException;
import audioresampler.audio.UnsupportedFormatException;
import audioresampler.audio.WaveController;
import audioresampler.audio.AudioFilter;
import audioresampler.audio.FastResamplingFilter;
import audioresampler.audio.ResamplingFilter;
import audioresampler.io.FileSink;
import audioresampler.io.FileSource;

public class ApplicationTest {	
	@Test
	public void testResampling()
	{
		test(new ResamplingFilter(8000));
	}
	
	@Test
	public void testFastResampling()
	{
		test(new FastResamplingFilter(8000));
	}
	
	private void test(AudioFilter filter)
	{
		try
		{
			final String InputFileName = "media/App1Test1Stereo16bits.wav";
			final String OutputFileName = "test_out.wav";
			
			FileSource input = new FileSource(InputFileName);
			FileSink output = new FileSink(OutputFileName);
			
			AudioController controller = new WaveController(input, output);
			controller.applyFilter(filter);
			controller.close();
			
			File outputFile = new File(OutputFileName);
			outputFile.deleteOnExit();
			
			if(!outputFile.exists() || outputFile.length() <= 0)
			{
				fail("Output File Not Found");
			}
			
		} catch (FileNotFoundException e) {
			fail("I/O Error");
		} catch (HeaderFormatException e) {
			fail("Invalid Input");
		} catch (UnsupportedFormatException e) {
			fail("Unsupported Input");
		} catch (Exception e) {
			fail("Unknown Exception");
		}
	}
}
