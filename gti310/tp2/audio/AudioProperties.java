package gti310.tp2.audio;

import java.nio.ByteOrder;

public class AudioProperties
{
	public static enum AudioFormat
	{
		NONE,
		WAVE_PCM
	};
	
	public short NumChannels;
	public int SampleRate;
	public short BitsPerSample;
	public ByteOrder ByteOrder;
	public AudioFormat Format;
	
	public int getChannelSize()
	{
		return (BitsPerSample / 8); 
	}
	
	public int getFrameSize()
	{
		return getChannelSize() * NumChannels;
	}
	
	public AudioProperties copy()
	{
		AudioProperties copy = new AudioProperties();
		copy.NumChannels = NumChannels;
		copy.SampleRate = SampleRate;
		copy.BitsPerSample = BitsPerSample;
		copy.Format = Format;
		copy.ByteOrder = ByteOrder;
		
		return copy;
	}
}
