package gti310.tp2.audio;

public class AudioProperties
{
	public short NumChannels;
	public int SampleRate;
	public short BitsPerSample;
	
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
		
		return copy;
	}
}
