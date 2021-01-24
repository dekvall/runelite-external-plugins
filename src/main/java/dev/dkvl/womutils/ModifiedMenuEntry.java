package dev.dkvl.womutils;

import lombok.Builder;
import lombok.Data;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;

@Data
class ModifiedMenuEntry extends MenuEntry
{
	ModifiedMenuEntry(MenuEntryAdded e)
	{
		ModifiedMenuEntry.builder()
			.option(e.getOption())
			.target(e.getTarget())
			.identifier(e.getIdentifier())
			.param0(e.getActionParam0())
			.param1(e.getActionParam1());
	}

	// Work around since there is no @Builder (yo use @SuperBuilder),
	// nor @AllArgsConstructor in MenuEntry
	@Builder(toBuilder = true)
	private ModifiedMenuEntry(String option,
							  String target,
							  int identifier,
							  int type,
							  int param0,
							  int param1,
							  boolean forceLeftClick)
	{
		setOption(option);
		setTarget(target);
		setIdentifier(identifier);
		setType(type);
		setParam0(param0);
		setParam1(param1);
		setForceLeftClick(forceLeftClick);
	}
}
