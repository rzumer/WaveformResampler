package gti310.tp2.audio;

import gti310.tp2.io.FileSink;
import gti310.tp2.io.FileSource;

public abstract class AudioController
{
	protected FileSource fileSource;
	protected FileSink fileSink;
	protected AudioProperties properties;
	
	public AudioController(FileSource source, FileSink sink) throws HeaderFormatException, UnsupportedFormatException
	{
		if(source == null || sink == null)
		{
			throw new IllegalArgumentException();
		}
		
		fileSource = source;
		fileSink = sink;
		properties = new AudioProperties();
		
		initializeProperties();
	}
	
	protected abstract void initializeProperties() throws HeaderFormatException, UnsupportedFormatException;
	
	public abstract void applyFilter(AudioFilter filter);
	
	public abstract void saveToFile(String outputFilePath);
	
	public void close()
	{
		if(fileSource != null)
		{
			fileSource.close();
		}
		
		if(fileSink != null)
		{
			fileSink.close();
		}
	}
}
