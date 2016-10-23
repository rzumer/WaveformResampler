package gti310.tp2.audio;

import java.lang.UnsupportedOperationException;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ResamplingFilter extends AudioFilter
{
	@Override
	public byte[] process(byte[] input)
	{
		return process(input, 8000); // Default output sample rate of 8000 Hz
	}
	
	public byte[] process(byte[] input, int outSampleRate)
	{
		if(outSampleRate == properties.SampleRate)
		{
			// No processing needed.
			return input;
		}
		else if(outSampleRate > properties.SampleRate)
		{
			// Upsampling is not implemented.
			throw new UnsupportedOperationException();
		}
		
		// Would be more efficient with linear interpolation to weigh samples directly using the input/output sample rate ratio.
		int frameSize = properties.getFrameSize();
		int sampleRateLCM = MathHelper.LeastCommonMultiple(properties.SampleRate, outSampleRate);
		
		// Bit Stuffing
		int padding = ((sampleRateLCM / properties.SampleRate) - 1) * frameSize;
		
		// Stuffed input contains the initial bytes, plus the zeroes which are between each frame.
		byte[] stuffedInput = new byte[input.length + (input.length / frameSize * padding) - padding];
		int stuffedInputPointer = 0;
		
		for(int i = 0; i < input.length; i+= frameSize)
		{
			byte[] frame = Arrays.copyOfRange(input, i, i + frameSize);
			
			for(byte b : frame)
			{
				stuffedInput[stuffedInputPointer] = b;
				stuffedInputPointer++;
			}
			
			// Do not stuff after the last frame.
			if(i < input.length - frameSize)
			{
				for(int j = 0; j < padding; j++)
				{
					stuffedInput[stuffedInputPointer] = 0;
					stuffedInputPointer++;
				}
			}
		}
		
		// Linear Interpolation
		for(int i = 0; i < stuffedInput.length; i += frameSize + padding)
		{
			int j = i + frameSize + padding;
			
			// Retrieve frames to interpolate.
			// If the left frame is the last one, do not interpolate. 
			byte[] leftFrame = Arrays.copyOfRange(stuffedInput, i, i + frameSize);
			byte[] rightFrame = j >= stuffedInput.length ? 
					null : Arrays.copyOfRange(stuffedInput, j, j + frameSize);

			for(int channel = 0; channel < properties.NumChannels; channel++)
			{
				int leftSample = ByteHelper.GetIntFromBytes(
						Arrays.copyOfRange(leftFrame, channel * properties.getChannelSize(), (channel + 1) * properties.getChannelSize()), 
						ByteOrder.LITTLE_ENDIAN, properties.BitsPerSample > 8);
				int rightSample = rightFrame == null ? 0 : ByteHelper.GetIntFromBytes(
						Arrays.copyOfRange(rightFrame, channel * properties.getChannelSize(), (channel + 1) * properties.getChannelSize()),
						ByteOrder.LITTLE_ENDIAN, properties.BitsPerSample > 8);
				
				for(int k = 0; k < (padding / frameSize); k++)
				{
					int index = i + (frameSize * (k + 1)) + (channel * properties.getChannelSize());
					
					if(index + properties.getChannelSize() <= stuffedInput.length)
					{
						int interpolatedSample = MathHelper.InterpolateLinear(leftSample, rightSample, 
								(float)(k + 1) / ((padding / frameSize) + 1));
						
						byte[] interpolatedBytes = properties.BitsPerSample == 8 ?
								new byte[] { (byte)interpolatedSample } : properties.BitsPerSample == 16 ? 
								ByteHelper.GetShortBytes((short)interpolatedSample, ByteOrder.LITTLE_ENDIAN) :
								ByteHelper.GetIntBytes(interpolatedSample, ByteOrder.LITTLE_ENDIAN);
						
						for(int l = 0; l < interpolatedBytes.length; l++)
						{
							stuffedInput[index + l] = interpolatedBytes[l]; 
						}
					}
				}
			}
		}
		
		// TODO proper low-pass filtering
		
		// Decimation
		int decimationRate = sampleRateLCM / outSampleRate;
		int outputSize = Math.round(input.length / ((float)properties.SampleRate / outSampleRate));
		
		// Add extra bytes if the end of the file is uneven.
		while(outputSize % frameSize != 0)
		{
			outputSize++;
		}
		
		byte[] downsampledInput = new byte[outputSize];
		int decimatedInputPointer = 0;
		
		for(int i = 0; i < stuffedInput.length; i += decimationRate * frameSize)
		{
			byte[] frame = Arrays.copyOfRange(stuffedInput, i, i + frameSize);
			
			for(byte b : frame)
			{
				downsampledInput[decimatedInputPointer] = b;
				decimatedInputPointer++;
			}
		}
		
		outProperties.SampleRate = outSampleRate;
		
		return downsampledInput;
	}
}
