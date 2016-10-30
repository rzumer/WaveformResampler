package audioresampler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import audioresampler.audio.AudioController;
import audioresampler.audio.FastResamplingFilter;
import audioresampler.audio.HeaderFormatException;
import audioresampler.audio.UnsupportedFormatException;
import audioresampler.audio.WaveController;
import audioresampler.io.FileSink;
import audioresampler.io.FileSource;

public class Application {

	/**
	 * Launch the application
	 * @param args This parameter is ignored
	 */
	public static void main(String args[])
	{
		long startTime = System.currentTimeMillis();

		if(args.length < 3)
		{
			System.err.println("Usage: AudioResampler <input> <output> <sample_rate>");
			return;
		}

		String inputFileName = args[0];
		String outputFileName = args[1];
		int sampleRate = Integer.parseInt(args[2]);

		try
		{
			File tempFile = File.createTempFile(outputFileName, null);
			tempFile.deleteOnExit();

			FileSource input = new FileSource(inputFileName);
			FileSink output = new FileSink(tempFile.getAbsolutePath());

			AudioController controller = new WaveController(input, output);

			controller.applyFilter(new FastResamplingFilter(sampleRate));
			controller.saveToFile(outputFileName);

			long processingTime = System.currentTimeMillis() - startTime;

			System.out.println(String.format("Done (%.2f seconds)", (double)processingTime / 1000));

		}
		catch (FileNotFoundException e)
		{
			System.err.println("File Access Error: " + e);
		}
		catch (HeaderFormatException e)
		{
			System.err.println("Invalid Input: " + e);
		}
		catch (UnsupportedFormatException e)
		{
			System.err.println("Unsupported Input: " + e);
		}
		catch (IOException e)
		{
			System.err.println("I/O Error: " + e);
		}
	}
}
