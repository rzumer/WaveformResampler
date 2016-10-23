package gti310.tp2.audio;

/**
 * An AudioFilter object will modify the input data and produce new values.
 * Each AudioFilter will specify its data source and data sink. Sources and
 * Sinks are not required : the filter may produce random data, or receive
 * data change it and do nothing with it afterwards.
 * 
 * For more information on WAVE file format visit :
 * http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
 * 
 * @author François Caron <francois.caron.7@ens.etsmtl.ca>
 */
public abstract class AudioFilter
{
	AudioProperties properties;
	
	/**
	 * Filter the input data.
	 * The function should make sure the input data is valid beforehand.
	 */
	public abstract byte[] process(byte[] input);
	
	public void setProperties(AudioProperties properties)
	{
		this.properties = properties;
	}
	
	public AudioProperties getProperties()
	{
		return properties;
	}
}
