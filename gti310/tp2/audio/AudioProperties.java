package gti310.tp2.audio;

public class AudioProperties
{
	public int NumChannels;
	public int SampleRate;
	public int BitsPerSample;
	
	public int getFrameSize()
	{
		return (BitsPerSample / 8) * NumChannels;
	}
}
