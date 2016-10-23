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
}
