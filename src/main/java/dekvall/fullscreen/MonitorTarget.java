package dekvall.fullscreen;

public enum MonitorTarget
{
	CURRENT("Current"),
	PRIMARY("Primary"),
	SECONDARY("Secondary"),
	SELECTED("Selected");

	private final String label;

	MonitorTarget(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
