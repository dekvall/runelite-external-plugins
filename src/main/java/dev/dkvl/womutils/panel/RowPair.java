package dev.dkvl.womutils.panel;

import lombok.Value;
import net.runelite.client.hiscore.HiscoreSkill;

@Value
public class RowPair
{
    HiscoreSkill skill;
    TableRow row;
}
