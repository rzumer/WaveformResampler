package gti310.tp2;

import java.io.FileNotFoundException;

import gti310.tp2.audio.*;
import gti310.tp2.io.*;

public class Application {

	/**
	 * Launch the application
	 * @param args This parameter is ignored
	 */
	public static void main(String args[]) {
		if(args.length < 2)
		{
			System.err.println("Usage: AudioResampler <input> <output>");
			return;
		}
		
		String inputFileName = args[0];
		String outputFileName = args[1];
		
		try {
			FileSource input = new FileSource(inputFileName);
			FileSink output = new FileSink(outputFileName);
			
			AudioController controller = new WaveController(input, output);
			controller.applyFilter(new ResamplingFilter());
			controller.close();
			
		} catch (FileNotFoundException e) {
			System.err.println("I/O Error");
			return;
		} catch (HeaderFormatException e) {
			//e.printStackTrace();
			System.err.println("Invalid Input");
		} catch (UnsupportedFormatException e) {
			// TODO Auto-generated catch block
			System.err.println("Unsupported Input");
		}
	}
}
