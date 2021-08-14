package dev.dkvl.womutils;

import dev.dkvl.womutils.beans.PlayerType;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.hiscore.HiscoreSkill;

public class Utils
{
	private static final Icon IRONMAN_ICON = new ImageIcon(ImageUtil.loadImageResource(WomUtilsPlugin.class, "ironman.png"));
	private static final Icon ULTIMATE_ICON = new ImageIcon(ImageUtil.loadImageResource(WomUtilsPlugin.class, "ultimate_ironman.png"));
	private static final Icon HARDCORE_ICON = new ImageIcon(ImageUtil.loadImageResource(WomUtilsPlugin.class, "hardcore_ironman.png"));
	private static final Icon REGULAR_ICON = new ImageIcon(ImageUtil.loadImageResource(WomUtilsPlugin.class, "build.png"));

    public static int getMinimumKc(HiscoreSkill boss)
    {
        switch (boss)
        {
            case MIMIC:
            case TZKAL_ZUK:
                return 2;
            case BRYOPHYTA:
            case CHAMBERS_OF_XERIC_CHALLENGE_MODE:
            case HESPORI:
            case OBOR:
            case SKOTIZO:
            case THE_CORRUPTED_GAUNTLET:
            case TZTOK_JAD:
                return 10;
            default:
                return 50;
        }
    }

    public static Icon getIcon(PlayerType type)
	{
		switch (type)
		{
			case IRONMAN:
				return IRONMAN_ICON;
			case HARDCORE:
				return HARDCORE_ICON;
			case ULTIMATE:
				return ULTIMATE_ICON;
			default:
				return REGULAR_ICON;
		}
	}
}
