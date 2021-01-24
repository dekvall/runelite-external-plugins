package dev.dkvl.womutils;

import lombok.Builder;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;

class ModifiedMenuEntry extends MenuEntry
{
	static ModifiedMenuEntry of(MenuEntryAdded e)
	{
		return ModifiedMenuEntry.builder()
			.option(e.getOption())
			.target(e.getTarget())
			.identifier(e.getIdentifier())
			.param0(e.getActionParam0())
			.param1(e.getActionParam1())
			.build();
	}

	// Work around since there is no @Builder (yo use @SuperBuilder),
	// nor @AllArgsConstructor in MenuEntry
	@Builder
	private ModifiedMenuEntry(String option, String target, int identifier, int type,
							  int param0, int param1, boolean forceLeftClick)
	{
		super();
		setOption(option);
		setTarget(target);
		setIdentifier(identifier);
		setType(type);
		setParam0(param0);
		setParam1(param1);
		setForceLeftClick(forceLeftClick);
	}
}
