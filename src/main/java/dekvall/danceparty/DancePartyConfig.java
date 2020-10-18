package dekvall.danceparty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Units;

@ConfigGroup("danceparty")
public interface DancePartyConfig extends Config
{
	@ConfigItem(
			keyName = "danceChance",
			name = "Dance Chance",
			description = "The chance a player has of dancing each tick.",
			position = 0
	)
	@Units(Units.PERCENT)
	default int danceChance()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "disableInPvp",
			name = "Disable in PvP",
			description = "Disable dance moves when entering dangerous situations :(",
			position = 1
	)
	default boolean disableInPvp()
	{
		return false;
	}

	@ConfigSection(
			name = "Only Dance When...",
			description = "If any of these are enabled, people will only dance if you've accomplished that thing!",
			closedByDefault = false,
			position = 2
	)
	String conditionalSection = "conditional";

	@ConfigSection(
			name = "Dance Moves",
			description = "Get down with the dance fever!",
			closedByDefault = false,
			position = 3
	)
	String danceSection = "dance";

	@ConfigSection(
			name = "Workout Moves",
			description = "#1 OSRS fitness inspiration",
			closedByDefault = false,
			position = 4
	)
	String workoutSection = "workout";

	@ConfigSection(
			name = "Other Moves",
			description = "Random other cute dance moves",
			closedByDefault = false,
			position = 5
	)
	String otherSection = "other";

	@ConfigItem(
			keyName = "partyOnLevelup",
			name = "...You Level Up",
			description = "Players will dance when you level up",
			section = conditionalSection,
			position = 0
	)
	default boolean partyOnLevelup()
	{
		return false;
	}

	@ConfigItem(
			keyName = "partyOnBossKill",
			name = "...You Get A Boss Kill",
			description = "Players will dance when you get a boss kill.",
			section = conditionalSection,
			position = 1
	)
	default boolean partyOnBossKill()
	{
		return false;
	}

	@ConfigItem(
			keyName = "partyOnRaidDone",
			name = "...You Finish A Raid",
			description = "Players will dance when you get a finish a raid or complete barrows.",
			section = conditionalSection,
			position = 2
	)
	default boolean partyOnRaidDone()
	{
		return false;
	}

	@ConfigItem(
			keyName = "partyOnPetDrop",
			name = "...You Get A Pet Drop",
			description = "Players will dance when you get a pet drop.",
			section = conditionalSection,
			position = 3
	)
	default boolean partyOnPetDrop()
	{
		return false;
	}

	@ConfigItem(
			keyName = "cheer",
			name = "Cheer",
			description = "LET'S GO!",
			section = danceSection
	)
	default boolean cheer()
	{
		return true;
	}

	@ConfigItem(
			keyName = "dance",
			name = "Dance",
			description = "Boogie down my guy.",
			section = danceSection
	)
	default boolean dance()
	{
		return true;
	}

	@ConfigItem(
			keyName = "jig",
			name = "Jig",
			description = "Gettin' jiggy with it.",
			section = danceSection
	)
	default boolean jig()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spin",
			name = "Spin",
			description = "Gettin' dizzy with it.",
			section = danceSection
	)
	default boolean spin()
	{
		return true;
	}

	@ConfigItem(
			keyName = "headbang",
			name = "Headbang",
			description = "Beelzebub has a devil put aside for me, for me, for me!",
			section = danceSection
	)
	default boolean headbang()
	{
		return true;
	}

	@ConfigItem(
			keyName = "jumpForJoy",
			name = "Jump For Joy",
			description = "Yahoo!",
			section = danceSection
	)
	default boolean jumpForJoy()
	{
		return true;
	}

	@ConfigItem(
			keyName = "zombieDance",
			name = "Zombie Dance",
			description = "Can't stop this thriller!",
			section = danceSection
	)
	default boolean zombieDance()
	{
		return true;
	}

	@ConfigItem(
			keyName = "smoothDance",
			name = "Smooth Dance",
			description = "She used to DM me in my CC...",
			section = danceSection
	)
	default boolean smoothDance()
	{
		return true;
	}

	@ConfigItem(
			keyName = "crazyDance",
			name = "Crazy Dance",
			description = "",
			section = danceSection
	)
	default boolean crazyDance()
	{
		return true;
	}

	@ConfigItem(
			keyName = "chickenDance",
			name = "Chicken Dance",
			description = "Bird is the word.",
			section = danceSection
	)
	default boolean chickenDance()
	{
		return true;
	}

	@ConfigItem(
			keyName = "airGuitar",
			name = "Air Guitar",
			description = "Hey now, you're a rockstar.",
			section = danceSection
	)
	default boolean airGuitar()
	{
		return true;
	}

	@ConfigItem(
			keyName = "goblinSalute",
			name = "Goblin Salute",
			description = "Give 'em the gobbie!",
			section = danceSection
	)
	default boolean goblinSalute()
	{
		return true;
	}

	@ConfigItem(
			keyName = "sitUp",
			name = "Sit Up",
			description = "Rock solid abs in no time!",
			section = workoutSection
	)
	default boolean sitUp()
	{
		return false;
	}

	@ConfigItem(
			keyName = "pushUp",
			name = "Push Up",
			description = "Drop and give me twenty!",
			section = workoutSection
	)
	default boolean pushUp()
	{
		return false;
	}

	@ConfigItem(
			keyName = "starJump",
			name = "Star Jump",
			description = "Work up a sweat!",
			section = workoutSection
	)
	default boolean starJump()
	{
		return false;
	}

	@ConfigItem(
			keyName = "jog",
			name = "Jog",
			description = "Get that blood pumpin'!",
			section = workoutSection
	)
	default boolean jog()
	{
		return false;
	}

	@ConfigItem(
			keyName = "gildedAltar",
			name = "Gilded Altar",
			description = "Clap them bones.",
			section = otherSection
	)
	default boolean gildedAltar()
	{
		return false;
	}

	@ConfigItem(
			keyName = "makeTeletab",
			name = "Make Teletab",
			description = "Praise the lord on high!",
			section = otherSection
	)
	default boolean makeTeletab()
	{
		return false;
	}

	@ConfigItem(
			keyName = "stringAmulet",
			name = "String Amulet",
			description = "Slow movin' swagger.",
			section = otherSection
	)
	default boolean stringAmulet()
	{
		return false;
	}

}
