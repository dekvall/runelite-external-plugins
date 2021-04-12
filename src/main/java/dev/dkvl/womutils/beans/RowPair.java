package dev.dkvl.womutils.beans;

import dev.dkvl.womutils.panel.TableRow;
import lombok.Value;
import net.runelite.http.api.hiscore.HiscoreSkill;

@Value
public class RowPair
{
    HiscoreSkill skill;
    TableRow row;
}
