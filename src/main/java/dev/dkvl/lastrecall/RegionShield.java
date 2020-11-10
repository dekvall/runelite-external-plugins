package dev.dkvl.lastrecall;

import com.google.common.collect.ImmutableMap;
import java.awt.image.BufferedImage;
import java.util.Map;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;

@Getter
enum RegionShield
{
	MISTHALIN("Misthalin", "misthalin.png"),
	KARAMJA("Karamja", "karamja.png"),
	ASGARNIA("Asgarnia", "asgarnia.png"),
	FREMENNIK("Fremennik Provinces", "fremennik.png"),
	KANDARIN("Kandarin", "kandarin.png"),
	DESERT("Kharidian Desert", "desert.png"),
	MORYTANIA("Morytania", "morytania.png"),
	TIRANNWN("Tirannwn", "tirannwn.png"),
	WILDERNESS("Wilderness", "wilderness.png");

	private static Map<String, BufferedImage> SHIELDS;

	private final String region;
	private final BufferedImage image;

	RegionShield(String region, String imgPath)
	{
		this.region = region;
		this.image = ImageUtil.getResourceStreamFromClass(LastRecallPlugin.class, imgPath);
	}

	static
	{
		ImmutableMap.Builder<String, BufferedImage> builder = new ImmutableMap.Builder<>();

		for (RegionShield shield : values())
		{
			builder.put(shield.getRegion(), shield.getImage());
		}

		SHIELDS = builder.build();
	}

	static BufferedImage getRegionShield(String region)
	{
		return SHIELDS.get(region);
	}
}
