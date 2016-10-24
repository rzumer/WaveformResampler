package gti310.tp2.audio;

import java.nio.ByteOrder;
import java.util.Arrays;

import gti310.tp2.audio.AudioProperties.AudioFormat;

public class ResamplingFilter extends AudioFilter
{	
	public ResamplingFilter(int outSampleRate)
	{
		super();
		
		outProperties.SampleRate = outSampleRate;
	}
	
	@Override
	public void setInputProperties(AudioProperties properties)
	{
		super.setInputProperties(properties);
		
		if(outProperties.SampleRate > 0)
		{
			int outSampleRate = outProperties.SampleRate;
			
			outProperties = properties.copy();
			outProperties.SampleRate = outSampleRate;
		}
	}

	@Override
	public byte[] process(byte[] input)
	{
		return process(input, outProperties.SampleRate); // Default output sample rate of 8000 Hz
	}
	
	public byte[] process(byte[] input, int outSampleRate)
	{
		if(outSampleRate <= 0)
		{
			throw new IllegalArgumentException();	
		}
		
		if(properties.Format != AudioFormat.WAVE_PCM)
		{
			try {
				throw new UnsupportedFormatException();
			} catch (UnsupportedFormatException e) {
				System.err.println("Unsupported Format");
			}
		}
		
		if(outSampleRate == properties.SampleRate)
		{
			// No processing needed.
			return input;
		}
		else if(outSampleRate > properties.SampleRate)
		{
			// See the interpolation note below for why this is not supported.
			throw new UnsupportedOperationException();
		}
		
		// Would be more efficient with linear interpolation to weigh samples directly using the input/output sample rate ratio.
		ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
		int frameSize = properties.getFrameSize();
		int sampleRateLCM = MathHelper.LeastCommonMultiple(properties.SampleRate, outSampleRate);
		
		// Bit Stuffing
		int padding = ((sampleRateLCM / properties.SampleRate) - 1) * frameSize;
		
		// Stuffed input contains the initial bytes, plus the zeroes which are between each frame.
		byte[] stuffedInput = new byte[input.length + (input.length / frameSize * padding)];
		int stuffedInputPointer = 0;
		
		for(int i = 0; i < input.length; i+= frameSize)
		{
			byte[] frame = Arrays.copyOfRange(input, i, i + frameSize);
			
			for(byte b : frame)
			{
				stuffedInput[stuffedInputPointer] = b;
				stuffedInputPointer++;
			}
			
			// Padding is also inserted after the last frame. This padding will not be interpolated.
			// It is used when upsampling, but never when downsampling.
			if(i <= (input.length - frameSize))
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

			// Note: upsampling using this method will leave uninterpolated bytes at the end of each segment.
			// This is because no interpolation can be done between segments.
			for(int channel = 0; channel < properties.NumChannels; channel++)
			{
				int leftSample = ByteHelper.GetIntFromBytes(
						Arrays.copyOfRange(leftFrame, channel * properties.getChannelSize(), (channel + 1) * properties.getChannelSize()), 
						byteOrder, properties.BitsPerSample > 8);
				int rightSample = rightFrame == null ? 0 : ByteHelper.GetIntFromBytes(
						Arrays.copyOfRange(rightFrame, channel * properties.getChannelSize(), (channel + 1) * properties.getChannelSize()),
						byteOrder, properties.BitsPerSample > 8);
				
				for(int k = 0; k < (padding / frameSize); k++)
				{
					int index = i + (frameSize * (k + 1)) + (channel * properties.getChannelSize());
					
					if(index + properties.getChannelSize() <= stuffedInput.length)
					{
						int interpolatedSample = MathHelper.InterpolateLinear(leftSample, rightSample, 
								(float)(k + 1) / ((padding / frameSize) + 1));
						
						byte[] interpolatedBytes = properties.BitsPerSample == 8 ?
								new byte[] { (byte)interpolatedSample } : properties.BitsPerSample == 16 ? 
								ByteHelper.GetShortBytes((short)interpolatedSample, byteOrder) :
								ByteHelper.GetIntBytes(interpolatedSample, byteOrder);
						
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
			
			// Prevent an array out of bounds on the final segment's trailing padding.
			if(decimatedInputPointer < outputSize + frame.length - 1)
			{
				for(byte b : frame)
				{
						downsampledInput[decimatedInputPointer] = b;
						decimatedInputPointer++;
				}
			}
		}
		
		outProperties.SampleRate = outSampleRate;
		
		return downsampledInput;
	}
}
