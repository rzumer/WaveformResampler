package gti310.tp2;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import gti310.tp2.audio.AudioController;
import gti310.tp2.audio.HeaderFormatException;
import gti310.tp2.audio.ResamplingFilter;
import gti310.tp2.audio.UnsupportedFormatException;
import gti310.tp2.audio.WaveController;
import gti310.tp2.io.FileSink;
import gti310.tp2.io.FileSource;

public class ApplicationTest {	
	@Test
	public void test()
	{
		try
		{
			final String InputFileName = "media/App1Test1Stereo16bits.wav";
			final String OutputFileName = "test_out.wav";
			
			FileSource input = new FileSource(InputFileName);
			FileSink output = new FileSink(OutputFileName);
			
			AudioController controller = new WaveController(input, output);
			controller.applyFilter(new ResamplingFilter(8000));
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
