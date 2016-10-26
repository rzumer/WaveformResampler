package audioresampler.audio;

public class UnsupportedFormatException extends Exception
{
	private static final long serialVersionUID = 9155584494061669271L;
	
	public UnsupportedFormatException()
	{
		super();
	}
	
	public UnsupportedFormatException(String message)
	{
		super(message);
	}
	
	public UnsupportedFormatException(Throwable cause)
	{
		super(cause);
	}
	
	public UnsupportedFormatException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
