package audioresampler.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A fast implementation of the resampling filter for linear resampling only.
 *
 * @author Raphaël Zumer <rzumer@gmail.com>
 */
public class FastResamplingFilter extends ResamplingFilter
{
	private byte[] lastFrameProcessed; // Represents the last frame of the last segment processed with the filter, used for interpolation.
	private double segmentOffset; // Used to balance the frame selection between segments.
	private double decimationRate; // Represents the number of input frames per output frame.
	private static int DecimalPlaces = 6; // Round weight and decimation rate to avoid floating point errors affecting frame selection

	public FastResamplingFilter(int outSampleRate)
	{
		super(outSampleRate);
	}

	@Override
	public void setInputProperties(AudioProperties properties)
	{
		super.setInputProperties(properties);

		// Set the decimation rate.
		decimationRate = MathHelper.Round((double)properties.SampleRate / outProperties.SampleRate, DecimalPlaces);

		// The segment offset is initialized at half the decimation rate.
		segmentOffset = MathHelper.Round(decimationRate / 2, DecimalPlaces);

		// When upsampling, get the extra number of samples needed by starting before the first sample.
		if(outProperties.SampleRate > properties.SampleRate)
		{
			segmentOffset *= -1;
		}
	}

	// O(n) * O(1) = O(n)
	@Override
	public byte[] process(byte[] input)
	{
		// 32-bit input is not yet supported.
		if(!validateInputParameters(0, 24))
		{
			throw new UnsupportedOperationException();
		}

		if(outProperties.SampleRate == properties.SampleRate)
		{
			// No processing needed.
			return input;
		}

		// Initialization
		int frameSize = properties.getFrameSize();

		ByteBuffer inputBuffer = ByteBuffer.wrap(input);
		inputBuffer.order(properties.ByteOrder);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// Iteration
		int frameCount = 0;

		// O(n) - Iterate through frames, dependent on n.
		for(;;)
		{
			double framePointer = MathHelper.Round(segmentOffset + (frameCount * decimationRate), DecimalPlaces);
			double framePointerDecimal = MathHelper.Round(framePointer % 1, DecimalPlaces);
			double weight = MathHelper.Round((framePointerDecimal + 1) % 1, DecimalPlaces);

			int leftFrameNumber = (int) Math.round(framePointer - weight);
			int rightFrameNumber = leftFrameNumber + 1;

			// The while check is insufficient to ensure that the new left frame
			// and the right frame, if one is needed, are present in this segment.
			if(input.length < (framePointerDecimal > 0 ? rightFrameNumber : leftFrameNumber) * frameSize + frameSize || (input.length == 0 && (int)Math.round(framePointerDecimal) >= 0))
			{
				break;
			}

			// Read samples in the left and right frames.
			// Left samples are null on the first frame when upsampling.
			int[] leftSamples = leftFrameNumber < 0 ? readFrameSamples(lastFrameProcessed) : readFrameSamples(inputBuffer, leftFrameNumber);
			int[] rightSamples = weight > 0 ? readFrameSamples(inputBuffer, rightFrameNumber) : null;

			// Perform linear interpolation for each sample.
			int[] interpolatedSamples = new int[properties.NumChannels];

			// O(1) - Iterate through channels, independent from n.
			for(int channel = 0; channel < properties.NumChannels; channel++)
			{
				interpolatedSamples[channel] = MathHelper.InterpolateLinear(leftSamples == null ? 0 : leftSamples[channel], rightSamples == null ? 0 : rightSamples[channel], weight);

				try
				{
					// Write interpolated samples to the output stream.
					if(properties.BitsPerSample <= 8)
					{
						outputStream.write((byte) interpolatedSamples[channel]);
					}
					else if(properties.BitsPerSample <= 16)
					{
						outputStream.write(ByteHelper.GetShortBytes((short) interpolatedSamples[channel], properties.ByteOrder));
					}
					else if(properties.BitsPerSample <= 24)
					{
						outputStream.write(ByteHelper.GetNumberBytes(interpolatedSamples[channel], properties.ByteOrder, 3));
					}
					else
					{
						// Assuming 32-bit signed integers.
						outputStream.write(ByteHelper.GetIntBytes(interpolatedSamples[channel], properties.ByteOrder));
					}
				}
				catch (IOException e)
				{
					System.err.println("Error Writing Output Stream");
				}
			}

			// Basic depth checking.
			if(frameCount++ > Math.round(input.length / (decimationRate * frameSize) + segmentOffset + frameSize))
			{
				throw new IndexOutOfBoundsException();
			}

			// Place the buffer back to the first byte of the left frame.
			inputBuffer.position(Math.max(leftFrameNumber * frameSize, 0));
		}

		// Return
		if(input.length >= frameSize)
		{
			lastFrameProcessed = Arrays.copyOfRange(input, input.length - frameSize, input.length);
			segmentOffset -= MathHelper.Round(((input.length / (decimationRate * frameSize) - (frameCount))) * decimationRate, DecimalPlaces);
		}

		outProperties.DataSize += outputStream.size();
		return outputStream.toByteArray();
	}

	// O(1)
	// Reads the contents of a frame into an integer per channel value, used for processing.
	private int[] readFrameSamples(ByteBuffer buffer, int frameNumber)
	{
		int position = frameNumber * properties.getFrameSize();

		if(buffer.limit() < position + properties.getFrameSize())
		{
			return null;
		}

		buffer.position(position);

		int[] samples = new int[properties.NumChannels];

		for(int channel = 0; channel < properties.NumChannels; channel++)
		{
			if(properties.BitsPerSample <= 8)
			{
				samples[channel] = buffer.get() & 0xff;
			}
			else if(properties.BitsPerSample <= 16)
			{
				samples[channel] = buffer.getShort();
			}
			else if(properties.BitsPerSample <= 24)
			{
				byte[] sampleBytes = new byte[3];
				buffer.get(sampleBytes);

				samples[channel] = ByteHelper.GetIntFromBytes(sampleBytes, properties.ByteOrder);
			}
			else
			{
				// Assuming 32-bit signed integers.
				samples[channel] = buffer.getInt();
			}
		}

		return samples;
	}

	private int[] readFrameSamples(byte[] frame)
	{
		if(frame == null)
		{
			return null;
		}

		return readFrameSamples(ByteBuffer.wrap(frame), 0);
	}
}
