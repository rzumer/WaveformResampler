package audioresampler.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A FileSink object writes the data sent to it to a file. If the file that
 * will we written has a specific header, the header should be sent to the file
 * as "data". The FileSink object has no knowledge of headers of tags. It only
 * writes bytes of data to a file.
 * 
 * @author François Caron <francois.caron.7@ens.etsmtl.ca>; Raphaël Zumer <rzumer@gmail.com>
 */
public class FileSink {
	
	/* The file's handler */
	private DataOutputStream _writer;
	private String location;
	private int bytesWritten;
	
	/**
	 * Create a new FileSink to write data to. The specified path must exist or
	 * instantiation will be cancelled. If a file with the same name exists,
	 * the file will be replaced without any warnings.
	 * 
	 * @param location The complete path to the file to create.
	 * @throws FileNotFoundException If the path is not valid.
	 */
	public FileSink(String location) throws FileNotFoundException
	{
		this.location = location;
		
		try
		{
			/* open new handler to the file */
			_writer = new DataOutputStream(
						new BufferedOutputStream(
							new FileOutputStream(location)));
		}
		catch (FileNotFoundException e)
		{
			throw e;
		}
	}
	
	/**
	 * Save what was written to the file and close the handle on it.
	 */
	public void close()
	{
		try
		{
			/* unreference the file */
			_writer.close();
		}
		catch (IOException e)
		{
			/* something went wrong */
		}
	}

	/**
	 * Append data to the file.
	 * @param data The data to write in the file.
	 */
	public void push(byte[] data)
	{
		try
		{
			/* append data to the end of the file */
			_writer.write(data);
			bytesWritten += data.length;
		}
		catch (IOException e)
		{
			/* something went wrong */
		}
	}
	
	public String getLocation()
	{
		return location;
	}
	
	public int getBytesWritten()
	{
		return bytesWritten;
	}
}
