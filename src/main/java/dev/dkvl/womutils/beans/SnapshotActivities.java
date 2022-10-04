package dev.dkvl.womutils.beans;

import lombok.Value;
import net.runelite.client.hiscore.HiscoreSkill;

@Value
public class SnapshotActivities
{
    Activity league_points;
    Activity bounty_hunter_hunter;
    Activity bounty_hunter_rogue;
    Activity clue_scrolls_all;
    Activity clue_scrolls_beginner;
    Activity clue_scrolls_easy;
    Activity clue_scrolls_medium;
    Activity clue_scrolls_hard;
    Activity clue_scrolls_elite;
    Activity clue_scrolls_master;
    Activity last_man_standing;
    Activity pvp_arena;
    Activity soul_wars_zeal;
    Activity guardians_of_the_rift;

    public Activity getActivity(HiscoreSkill skill)
    {
        switch (skill)
        {
            case LEAGUE_POINTS:
                return getLeague_points();
            case BOUNTY_HUNTER_HUNTER:
                return getBounty_hunter_hunter();
            case BOUNTY_HUNTER_ROGUE:
                return getBounty_hunter_rogue();
            case CLUE_SCROLL_ALL:
                return getClue_scrolls_all();
            case CLUE_SCROLL_BEGINNER:
                return getClue_scrolls_beginner();
            case CLUE_SCROLL_EASY:
                return getClue_scrolls_easy();
            case CLUE_SCROLL_MEDIUM:
                return getClue_scrolls_medium();
            case CLUE_SCROLL_HARD:
                return getClue_scrolls_hard();
            case CLUE_SCROLL_ELITE:
                return getClue_scrolls_elite();
            case CLUE_SCROLL_MASTER:
                return getClue_scrolls_master();
            case LAST_MAN_STANDING:
                return getLast_man_standing();
            case PVP_ARENA_RANK:
                return getPvp_arena();
            case SOUL_WARS_ZEAL:
                return getSoul_wars_zeal();
            case RIFTS_CLOSED:
                return getGuardians_of_the_rift();
            default:
                throw new IllegalArgumentException("Invalid hiscore minigame");
        }
    }
}
