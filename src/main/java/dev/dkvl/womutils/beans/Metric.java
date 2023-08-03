package dev.dkvl.womutils.beans;

import com.google.gson.annotations.SerializedName;
import dev.dkvl.womutils.WomUtilsPlugin;
import java.awt.image.BufferedImage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

@AllArgsConstructor
public enum Metric
{
	@SerializedName("abyssal_sire")
	ABYSSAL_SIRE(HiscoreSkill.ABYSSAL_SIRE),
	@SerializedName("agility")
	AGILITY(HiscoreSkill.AGILITY),
	@SerializedName("alchemical_hydra")
	ALCHEMICAL_HYDRA(HiscoreSkill.ALCHEMICAL_HYDRA),
	@SerializedName("artio")
	ARTIO(HiscoreSkill.ARTIO),
	@SerializedName("attack")
	ATTACK(HiscoreSkill.ATTACK),
	@SerializedName("barrows_chests")
	BARROWS_CHESTS(HiscoreSkill.BARROWS_CHESTS),
	@SerializedName("bounty_hunter_hunter")
	BOUNTY_HUNTER_HUNTER(HiscoreSkill.BOUNTY_HUNTER_HUNTER),
	@SerializedName("bounty_hunter_rogue")
	BOUNTY_HUNTER_ROGUE(HiscoreSkill.BOUNTY_HUNTER_ROGUE),
	@SerializedName("bryophyta")
	BRYOPHYTA(HiscoreSkill.BRYOPHYTA),
	@SerializedName("callisto")
	CALLISTO(HiscoreSkill.CALLISTO),
	@SerializedName("calvarion")
	CALVARION(HiscoreSkill.CALVARION),
	@SerializedName("cerberus")
	CERBERUS(HiscoreSkill.CERBERUS),
	@SerializedName("chambers_of_xeric_challenge_mode")
	CHAMBERS_OF_XERIC_CHALLENGE_MODE(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE),
	@SerializedName("chambers_of_xeric")
	CHAMBERS_OF_XERIC(HiscoreSkill.CHAMBERS_OF_XERIC),
	@SerializedName("chaos_elemental")
	CHAOS_ELEMENTAL(HiscoreSkill.CHAOS_ELEMENTAL),
	@SerializedName("chaos_fanatic")
	CHAOS_FANATIC(HiscoreSkill.CHAOS_FANATIC),
	@SerializedName("clue_scrolls_all")
	CLUE_SCROLLS_ALL(HiscoreSkill.CLUE_SCROLL_ALL),
	@SerializedName("clue_scrolls_beginner")
	CLUE_SCROLLS_BEGINNER(HiscoreSkill.CLUE_SCROLL_BEGINNER),
	@SerializedName("clue_scrolls_easy")
	CLUE_SCROLLS_EASY(HiscoreSkill.CLUE_SCROLL_EASY),
	@SerializedName("clue_scrolls_elite")
	CLUE_SCROLLS_ELITE(HiscoreSkill.CLUE_SCROLL_ELITE),
	@SerializedName("clue_scrolls_hard")
	CLUE_SCROLLS_HARD(HiscoreSkill.CLUE_SCROLL_HARD),
	@SerializedName("clue_scrolls_master")
	CLUE_SCROLLS_MASTER(HiscoreSkill.CLUE_SCROLL_MASTER),
	@SerializedName("clue_scrolls_medium")
	CLUE_SCROLLS_MEDIUM(HiscoreSkill.CLUE_SCROLL_MEDIUM),
	@SerializedName("commander_zilyana")
	COMMANDER_ZILYANA(HiscoreSkill.COMMANDER_ZILYANA),
	@SerializedName("construction")
	CONSTRUCTION(HiscoreSkill.CONSTRUCTION),
	@SerializedName("cooking")
	COOKING(HiscoreSkill.COOKING),
	@SerializedName("corporeal_beast")
	CORPOREAL_BEAST(HiscoreSkill.CORPOREAL_BEAST),
	@SerializedName("crafting")
	CRAFTING(HiscoreSkill.CRAFTING),
	@SerializedName("crazy_archaeologist")
	CRAZY_ARCHAEOLOGIST(HiscoreSkill.CRAZY_ARCHAEOLOGIST),
	@SerializedName("dagannoth_prime")
	DAGANNOTH_PRIME(HiscoreSkill.DAGANNOTH_PRIME),
	@SerializedName("dagannoth_rex")
	DAGANNOTH_REX(HiscoreSkill.DAGANNOTH_REX),
	@SerializedName("dagannoth_supreme")
	DAGANNOTH_SUPREME(HiscoreSkill.DAGANNOTH_SUPREME),
	@SerializedName("defence")
	DEFENCE(HiscoreSkill.DEFENCE),
	@SerializedName("deranged_archaeologist")
	DERANGED_ARCHAEOLOGIST(HiscoreSkill.DERANGED_ARCHAEOLOGIST),
	@SerializedName("duke_sucellus")
	DUKE_SUCELLUS(HiscoreSkill.DUKE_SUCELLUS),
	@SerializedName("ehb")
	EHB(null),
	@SerializedName("ehp")
	EHP(null),
	@SerializedName("farming")
	FARMING(HiscoreSkill.FARMING),
	@SerializedName("firemaking")
	FIREMAKING(HiscoreSkill.FIREMAKING),
	@SerializedName("fishing")
	FISHING(HiscoreSkill.FISHING),
	@SerializedName("fletching")
	FLETCHING(HiscoreSkill.FLETCHING),
	@SerializedName("general_graardor")
	GENERAL_GRAARDOR(HiscoreSkill.GENERAL_GRAARDOR),
	@SerializedName("giant_mole")
	GIANT_MOLE(HiscoreSkill.GIANT_MOLE),
	@SerializedName("grotesque_guardians")
	GROTESQUE_GUARDIANS(HiscoreSkill.GROTESQUE_GUARDIANS),
	@SerializedName("guardians_of_the_rift")
	GUARDIANS_OF_THE_RIFT(HiscoreSkill.RIFTS_CLOSED),
	@SerializedName("herblore")
	HERBLORE(HiscoreSkill.HERBLORE),
	@SerializedName("hespori")
	HESPORI(HiscoreSkill.HESPORI),
	@SerializedName("hitpoints")
	HITPOINTS(HiscoreSkill.HITPOINTS),
	@SerializedName("hunter")
	HUNTER(HiscoreSkill.HUNTER),
	@SerializedName("kalphite_queen")
	KALPHITE_QUEEN(HiscoreSkill.KALPHITE_QUEEN),
	@SerializedName("king_black_dragon")
	KING_BLACK_DRAGON(HiscoreSkill.KING_BLACK_DRAGON),
	@SerializedName("kraken")
	KRAKEN(HiscoreSkill.KRAKEN),
	@SerializedName("kreearra")
	KREEARRA(HiscoreSkill.KREEARRA),
	@SerializedName("kril_tsutsaroth")
	KRIL_TSUTSAROTH(HiscoreSkill.KRIL_TSUTSAROTH),
	@SerializedName("last_man_standing")
	LAST_MAN_STANDING(HiscoreSkill.LAST_MAN_STANDING),
	@SerializedName("pvp_arena")
	PVP_ARENA(HiscoreSkill.PVP_ARENA_RANK),
	@SerializedName("league_points")
	LEAGUE_POINTS(HiscoreSkill.LEAGUE_POINTS),
	@SerializedName("magic")
	MAGIC(HiscoreSkill.MAGIC),
	@SerializedName("mimic")
	MIMIC(HiscoreSkill.MIMIC),
	@SerializedName("mining")
	MINING(HiscoreSkill.MINING),
	@SerializedName("nex")
	NEX(HiscoreSkill.NEX),
	@SerializedName("nightmare")
	NIGHTMARE(HiscoreSkill.NIGHTMARE),
	@SerializedName("obor")
	OBOR(HiscoreSkill.OBOR),
	@SerializedName("overall")
	OVERALL(HiscoreSkill.OVERALL),
	@SerializedName("phantom_muspah")
	PHANTOM_MUSPAH(HiscoreSkill.PHANTOM_MUSPAH),
	@SerializedName("phosanis_nightmare")
	PHOSANIS_NIGHTMARE(HiscoreSkill.PHOSANIS_NIGHTMARE),
	@SerializedName("prayer")
	PRAYER(HiscoreSkill.PRAYER),
	@SerializedName("ranged")
	RANGED(HiscoreSkill.RANGED),
	@SerializedName("runecrafting")
	RUNECRAFTING(HiscoreSkill.RUNECRAFT),
	@SerializedName("sarachnis")
	SARACHNIS(HiscoreSkill.SARACHNIS),
	@SerializedName("scorpia")
	SCORPIA(HiscoreSkill.SCORPIA),
	@SerializedName("skotizo")
	SKOTIZO(HiscoreSkill.SKOTIZO),
	@SerializedName("spindel")
	SPINDEL(HiscoreSkill.SPINDEL),
	@SerializedName("slayer")
	SLAYER(HiscoreSkill.SLAYER),
	@SerializedName("smithing")
	SMITHING(HiscoreSkill.SMITHING),
	@SerializedName("soul_wars_zeal")
	SOUL_WARS_ZEAL(HiscoreSkill.SOUL_WARS_ZEAL),
	@SerializedName("strength")
	STRENGTH(HiscoreSkill.STRENGTH),
	@SerializedName("tempoross")
	TEMPOROSS(HiscoreSkill.TEMPOROSS),
	@SerializedName("theatre_of_blood_hard_mode")
	THEATRE_OF_BLOOD_HARD_MODE(HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE),
	@SerializedName("theatre_of_blood")
	THEATRE_OF_BLOOD(HiscoreSkill.THEATRE_OF_BLOOD),
	@SerializedName("the_corrupted_gauntlet")
	THE_CORRUPTED_GAUNTLET(HiscoreSkill.THE_CORRUPTED_GAUNTLET),
	@SerializedName("the_leviathan")
	THE_LEVIATHAN(HiscoreSkill.THE_LEVIATHAN),
	@SerializedName("the_whisperer")
	THE_WHISPERER(HiscoreSkill.THE_WHISPERER),
	@SerializedName("the_gauntlet")
	THE_GAUNTLET(HiscoreSkill.THE_GAUNTLET),
	@SerializedName("thermonuclear_smoke_devil")
	THERMONUCLEAR_SMOKE_DEVIL(HiscoreSkill.THERMONUCLEAR_SMOKE_DEVIL),
	@SerializedName("tombs_of_amascut")
	TOMBS_OF_AMASCUT(HiscoreSkill.TOMBS_OF_AMASCUT),
	@SerializedName("tombs_of_amascut_expert")
	TOMBS_OF_AMASCUT_EXPERT(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT),
	@SerializedName("thieving")
	THIEVING(HiscoreSkill.THIEVING),
	@SerializedName("tzkal_zuk")
	TZKAL_ZUK(HiscoreSkill.TZKAL_ZUK),
	@SerializedName("tztok_jad")
	TZTOK_JAD(HiscoreSkill.TZTOK_JAD),
	@SerializedName("vardorvis")
	VARDORVIS(HiscoreSkill.VARDORVIS),
	@SerializedName("venenatis")
	VENENATIS(HiscoreSkill.VENENATIS),
	@SerializedName("vetion")
	VETION(HiscoreSkill.VETION),
	@SerializedName("vorkath")
	VORKATH(HiscoreSkill.VORKATH),
	@SerializedName("wintertodt")
	WINTERTODT(HiscoreSkill.WINTERTODT),
	@SerializedName("woodcutting")
	WOODCUTTING(HiscoreSkill.WOODCUTTING),
	@SerializedName("zalcano")
	ZALCANO(HiscoreSkill.ZALCANO),
	@SerializedName("zulrah")
	ZULRAH(HiscoreSkill.ZULRAH)
	;

	@Getter
	private HiscoreSkill hiscoreSkill;

	public BufferedImage loadImage()
	{
		return ImageUtil.loadImageResource(WomUtilsPlugin.class, "metrics/" + name().toLowerCase() + ".png");
	}

	public String getName()
	{
		if (hiscoreSkill == null)
		{
			return this.toString();
		}
		return hiscoreSkill.getName();
	}

	public HiscoreSkillType getType()
	{
		if (hiscoreSkill == null)
		{
			return null;
		}
		return hiscoreSkill.getType();
	}
}
