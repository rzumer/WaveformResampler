package audioresampler.audio;

/**
 * Represents a filter able to accept input audio data,
 * performs deterministic processing on it, and returns the output.
 * 
 * @author Raphaël Zumer <rzumer@gmail.com>
 */
public abstract class AudioFilter
{
	AudioProperties properties;
	AudioProperties outProperties;
	
	public AudioFilter()
	{
		properties = new AudioProperties();
		outProperties = new AudioProperties();
	}
	
	/**
	 * Filter the input data.
	 * The function should make sure the input data is valid beforehand.
	 */
	public abstract byte[] process(byte[] input);
	
	public void setInputProperties(AudioProperties properties)
	{
		this.properties = properties;
	}
	
	public AudioProperties getOutputProperties()
	{
		return outProperties;
	}
}
