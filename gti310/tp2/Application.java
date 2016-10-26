package gti310.tp2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import gti310.tp2.audio.*;
import gti310.tp2.io.*;

public class Application {

	/**
	 * Launch the application
	 * @param args This parameter is ignored
	 */
	public static void main(String args[])
	{
		if(args.length < 2)
		{
			System.err.println("Usage: AudioResampler <input> <output>");
			return;
		}
		
		String inputFileName = args[0];
		String outputFileName = args[1];
		
		try
		{
			File tempFile = File.createTempFile(outputFileName, null);
			tempFile.deleteOnExit();
			
			FileSource input = new FileSource(inputFileName);
			FileSink output = new FileSink(tempFile.getAbsolutePath());
			
			long startTime = System.currentTimeMillis();
			
			AudioController controller = new WaveController(input, output);
			controller.applyFilter(new FastResamplingFilter(8000));
			controller.saveToFile(outputFileName);
			
			long processingTime = System.currentTimeMillis() - startTime;
			
			System.out.println(String.format("Done (%.2f seconds)", (double)processingTime / 1000));
			
		} catch (FileNotFoundException e) {
			System.err.println("File Access Error");
		} catch (HeaderFormatException e) {
			System.err.println("Invalid Input");
		} catch (UnsupportedFormatException e) {
			System.err.println("Unsupported Input");
		} catch (IOException e) {
			System.err.println("I/O Error");
		}
	}
}
