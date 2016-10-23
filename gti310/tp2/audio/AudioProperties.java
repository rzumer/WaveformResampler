package gti310.tp2.audio;

public class AudioProperties
{
	public short NumChannels;
	public int SampleRate;
	public short BitsPerSample;
	
	public int getFrameSize()
	{
		return (BitsPerSample / 8) * NumChannels;
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
