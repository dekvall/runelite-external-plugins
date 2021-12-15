package dev.dkvl.womutils.util;

import dev.dkvl.womutils.WomUtilsPlugin;
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
                return 1;
            case BRYOPHYTA:
            case CHAMBERS_OF_XERIC_CHALLENGE_MODE:
            case HESPORI:
            case OBOR:
            case SKOTIZO:
            case THE_CORRUPTED_GAUNTLET:
            case TZTOK_JAD:
                return 5;
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

	public static String ordinalOf(int i) {
		String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
		switch (i % 100) {
			case 11:
			case 12:
			case 13:
				return i + "th";
			default:
				return i + suffixes[i % 10];
		}
	}
}
