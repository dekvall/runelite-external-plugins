package dev.dkvl.womutils.beans;

import lombok.Value;
import net.runelite.http.api.hiscore.HiscoreSkill;

@Value
public class Snapshot
{
	String createdAt;
	String importedAt;
	Skill overall;
	Skill attack;
	Skill defence;
	Skill strength;
	Skill hitpoints;
	Skill ranged;
	Skill prayer;
	Skill magic;
	Skill cooking;
	Skill woodcutting;
	Skill fletching;
	Skill fishing;
	Skill firemaking;
	Skill crafting;
	Skill smithing;
	Skill mining;
	Skill herblore;
	Skill agility;
	Skill thieving;
	Skill slayer;
	Skill farming;
	Skill runecrafting;
	Skill hunter;
	Skill construction;
	Minigame league_points;
	Minigame bounty_hunter_hunter;
	Minigame bounty_hunter_rogue;
	Minigame clue_scrolls_all;
	Minigame clue_scrolls_beginner;
	Minigame clue_scrolls_easy;
	Minigame clue_scrolls_medium;
	Minigame clue_scrolls_hard;
	Minigame clue_scrolls_elite;
	Minigame clue_scrolls_master;
	Minigame last_man_standing;
	Minigame soul_wars_zeal;
	Boss abyssal_sire;
	Boss alchemical_hydra;
	Boss barrows_chests;
	Boss bryophyta;
	Boss callisto;
	Boss cerberus;
	Boss chambers_of_xeric;
	Boss chambers_of_xeric_challenge_mode;
	Boss chaos_elemental;
	Boss chaos_fanatic;
	Boss commander_zilyana;
	Boss corporeal_beast;
	Boss crazy_archaeologist;
	Boss dagannoth_prime;
	Boss dagannoth_rex;
	Boss dagannoth_supreme;
	Boss deranged_archaeologist;
	Boss general_graardor;
	Boss giant_mole;
	Boss grotesque_guardians;
	Boss hespori;
	Boss kalphite_queen;
	Boss king_black_dragon;
	Boss kraken;
	Boss kreearra;
	Boss kril_tsutsaroth;
	Boss mimic;
	Boss nightmare;
	Boss obor;
	Boss sarachnis;
	Boss scorpia;
	Boss skotizo;
	Boss tempoross;
	Boss the_gauntlet;
	Boss the_corrupted_gauntlet;
	Boss theatre_of_blood;
	Boss thermonuclear_smoke_devil;
	Boss tzkal_zuk;
	Boss tztok_jad;
	Boss venenatis;
	Boss vetion;
	Boss vorkath;
	Boss wintertodt;
	Boss zalcano;
	Boss zulrah;
	VirtualSkill ehp;
	VirtualSkill ehb;

	public Skill getSkill(HiscoreSkill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return getAttack();
			case DEFENCE:
				return getDefence();
			case STRENGTH:
				return getStrength();
			case HITPOINTS:
				return getHitpoints();
			case RANGED:
				return getRanged();
			case PRAYER:
				return getPrayer();
			case MAGIC:
				return getMagic();
			case COOKING:
				return getCooking();
			case WOODCUTTING:
				return getWoodcutting();
			case FLETCHING:
				return getFletching();
			case FISHING:
				return getFishing();
			case FIREMAKING:
				return getFiremaking();
			case CRAFTING:
				return getCrafting();
			case SMITHING:
				return getSmithing();
			case MINING:
				return getMining();
			case HERBLORE:
				return getHerblore();
			case AGILITY:
				return getAgility();
			case THIEVING:
				return getThieving();
			case SLAYER:
				return getSlayer();
			case FARMING:
				return getFarming();
			case RUNECRAFT:
				return getRunecrafting();
			case HUNTER:
				return getHunter();
			case CONSTRUCTION:
				return getConstruction();
			case OVERALL:
				return getOverall();
			default:
				throw new IllegalArgumentException("Invalid hiscore skill");
		}
	}

	public Minigame getMinigame(HiscoreSkill skill)
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
			case SOUL_WARS_ZEAL:
				return getSoul_wars_zeal();
			default:
				throw new IllegalArgumentException("Invalid hiscore minigame");
		}

	}

	public Boss getBoss(HiscoreSkill skill)
	{
		switch(skill)
		{
			case ABYSSAL_SIRE:
				return getAbyssal_sire();
			case ALCHEMICAL_HYDRA:
				return getAlchemical_hydra();
			case BARROWS_CHESTS:
				return getBarrows_chests();
			case BRYOPHYTA:
				return getBryophyta();
			case CALLISTO:
				return getCallisto();
			case CERBERUS:
				return getCerberus();
			case CHAMBERS_OF_XERIC:
				return getChambers_of_xeric();
			case CHAMBERS_OF_XERIC_CHALLENGE_MODE:
				return getChambers_of_xeric_challenge_mode();
			case CHAOS_ELEMENTAL:
				return getChaos_elemental();
			case CHAOS_FANATIC:
				return getChaos_fanatic();
			case COMMANDER_ZILYANA:
				return getCommander_zilyana();
			case CORPOREAL_BEAST:
				return getCorporeal_beast();
			case CRAZY_ARCHAEOLOGIST:
				return getCrazy_archaeologist();
			case DAGANNOTH_PRIME:
				return getDagannoth_prime();
			case DAGANNOTH_REX:
				return getDagannoth_rex();
			case DAGANNOTH_SUPREME:
				return getDagannoth_supreme();
			case DERANGED_ARCHAEOLOGIST:
				return getDeranged_archaeologist();
			case GENERAL_GRAARDOR:
				return getGeneral_graardor();
			case GIANT_MOLE:
				return getGiant_mole();
			case GROTESQUE_GUARDIANS:
				return getGrotesque_guardians();
			case HESPORI:
				return hespori;
			case KALPHITE_QUEEN:
				return getKalphite_queen();
			case KING_BLACK_DRAGON:
				return getKing_black_dragon();
			case KRAKEN:
				return getKraken();
			case KREEARRA:
				return getKreearra();
			case KRIL_TSUTSAROTH:
				return getKril_tsutsaroth();
			case MIMIC:
				return getMimic();
			case NIGHTMARE:
				return getNightmare();
			case OBOR:
				return getObor();
			case SARACHNIS:
				return getSarachnis();
			case SCORPIA:
				return getScorpia();
			case SKOTIZO:
				return getSkotizo();
			case TEMPOROSS:
				return getTempoross();
			case THE_GAUNTLET:
				return getThe_gauntlet();
			case THE_CORRUPTED_GAUNTLET:
				return getThe_corrupted_gauntlet();
			case THEATRE_OF_BLOOD:
				return getTheatre_of_blood();
			case THERMONUCLEAR_SMOKE_DEVIL:
				return getThermonuclear_smoke_devil();
			case TZKAL_ZUK:
				return getTzkal_zuk();
			case TZTOK_JAD:
				return getTztok_jad();
			case VENENATIS:
				return getVenenatis();
			case VETION:
				return getVetion();
			case VORKATH:
				return getVorkath();
			case WINTERTODT:
				return getWintertodt();
			case ZALCANO:
				return getZalcano();
			case ZULRAH:
				return getZulrah();
			default:
				throw new IllegalArgumentException("Invalid hiscore boss");
		}
	}
}
