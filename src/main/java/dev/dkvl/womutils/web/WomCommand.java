package dev.dkvl.womutils.web;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WomCommand
{
	EHP("!ehp", "ehp", "Efficient hours played: "),
	EHB("!ehb", "ehb", "Efficient hours bossed: "),
	TTM("!ttm", "ttm","Time to max: "),
	TT200M("!tt200m", "tt200m","Time to 200m: "),
	;

	private final String command;
	private final String field;
	private final String message;


	static WomCommand fromCommand(String command)
	{
		for (WomCommand c : values())
		{
			if (c.getCommand().equals(command.toLowerCase()))
			{
				return c;
			}
		}
		return null;
	}
}
