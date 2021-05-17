package dev.dkvl.womutils;

import net.runelite.http.api.hiscore.HiscoreSkill;

public class Utils
{
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
}
