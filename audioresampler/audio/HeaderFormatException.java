package audioresampler.audio;

public class HeaderFormatException extends Exception
{
	private static final long serialVersionUID = 9155584494061669271L;
	
	public HeaderFormatException()
	{
		super();
	}
	
	public HeaderFormatException(String message)
	{
		super(message);
	}
	
	public HeaderFormatException(Throwable cause)
	{
		super(cause);
	}
	
	public HeaderFormatException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
