package dev.dkvl.womutils.panel;

import lombok.Value;
import net.runelite.http.api.hiscore.HiscoreSkill;

@Value
public class RowPair
{
    HiscoreSkill skill;
    TableRow row;
}
