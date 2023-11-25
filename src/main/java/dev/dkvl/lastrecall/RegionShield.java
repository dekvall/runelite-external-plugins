package dev.dkvl.lastrecall;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
enum RegionShield
{
	MISTHALIN("Misthalin", 2731),
	KARAMJA("Karamja", 2732),
	ASGARNIA("Asgarnia", 2733),
	FREMENNIK("Fremennik Province", 2738),
	KANDARIN("Kandarin", 2737),
	DESERT("Kharidian Desert", 2734),
	MORYTANIA("Morytania", 2735),
	TIRANNWN("Tirannwn", 2739),
	WILDERNESS("Wilderness", 2736),
	KOUREND("Great Kourend and Kebos Lowlands", 5468);

	private static Map<String, Integer> SHIELDS;

	private final String region;
	private final int spriteId;

	static
	{
		ImmutableMap.Builder<String, Integer> builder = new ImmutableMap.Builder<>();

		for (RegionShield shield : values())
		{
			builder.put(shield.getRegion(), shield.getSpriteId());
		}

		SHIELDS = builder.build();
	}

	static int getRegionShield(String region)
	{
		return SHIELDS.get(region);
	}
}
