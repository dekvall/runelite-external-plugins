package dekvall.fullscreen;

public enum Mode
{
	EXCLUSIVE("Exclusive"),
	BORDERLESS("Borderless"),
	WINDOWED("Windowed");

	private final String label;

	Mode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
