package audioresampler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import audioresampler.audio.AudioController;
import audioresampler.audio.AudioFilter;
import audioresampler.audio.FastResamplingFilter;
import audioresampler.audio.HeaderFormatException;
import audioresampler.audio.ResamplingFilter;
import audioresampler.audio.UnsupportedFormatException;
import audioresampler.audio.WaveController;
import audioresampler.io.FileSink;
import audioresampler.io.FileSource;

public class ApplicationTest
{
	@Test
	public void testDownsampling()
	{
		test(new ResamplingFilter(8000), 5147260, 933744);
	}

	@Test
	public void testFastDownsampling()
	{
		test(new FastResamplingFilter(8000), 5147260, 933744);
	}

	@Test
	public void testFastUpsampling()
	{
		test(new FastResamplingFilter(96000), 5147260, 11204920);
	}

	private void test(AudioFilter filter, int expectedInputSize, int expectedOutputSize)
	{
		try
		{
			final String InputFileName = "media/App1Test1Stereo16bits.wav";
			final String OutputFileName = "test_out.wav";

			FileSource input = new FileSource(InputFileName);
			FileSink output = new FileSink("temp.wav");

			File inputFile = new File(InputFileName);
			File tempFile = new File(output.getLocation());
			tempFile.deleteOnExit();

			AudioController controller = new WaveController(input, output);

			assertEquals(expectedInputSize + 44, inputFile.length());
			assertEquals(expectedInputSize, controller.getProperties().DataSize);

			controller.applyFilter(filter);

			assertEquals(expectedOutputSize, controller.getProperties().DataSize);

			controller.saveToFile(OutputFileName);
			controller.close();

			File outputFile = new File(OutputFileName);
			outputFile.deleteOnExit();

			if(!outputFile.exists())
			{
				fail("Output File Not Found");
			}

			assertEquals(expectedOutputSize + 44, outputFile.length());

		}
		catch (FileNotFoundException e)
		{
			fail("I/O Error");
		}
		catch (HeaderFormatException e)
		{
			fail("Invalid Input");
		}
		catch (UnsupportedFormatException e)
		{
			fail("Unsupported Input");
		}
		catch (Exception e)
		{
			fail("Unknown Exception");
		}
	}
}
