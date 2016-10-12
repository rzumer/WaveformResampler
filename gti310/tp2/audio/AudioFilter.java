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
 * @author Fran�ois Caron <francois.caron.7@ens.etsmtl.ca>
 */
public interface AudioFilter {

	/**
	 * Filter the input data.
	 * The function should make sure the input data is valid beforehand.
	 */
	void process();
}
