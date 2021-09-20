package dev.dkvl.womutils.ui;

import dev.dkvl.womutils.WomUtilsPlugin;
import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ImageUtil;

public class PlaceHolderCompetitionInfobox extends InfoBox
{
	private static final BufferedImage LOGO = ImageUtil.loadImageResource(WomUtilsPlugin.class, "wom-logo.png");
	final WomUtilsPlugin plugin;

	public PlaceHolderCompetitionInfobox(WomUtilsPlugin plugin)
	{
		super(LOGO, plugin);
		this.plugin = plugin;

		this.getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_INFOBOX, WomUtilsPlugin.SHOW_ALL_COMPETITIONS, "Wise Old Man"));
	}

	@Override
	public String getTooltip()
	{
		return "You have " + plugin.countHiddenInfoboxes() + " hidden competitions</br>You can show them by shift-clicking this box";
	}

	@Override
	public Color getTextColor()
	{
		return Color.BLUE;
	}

	@Override
	public String getText()
	{
		return "" + plugin.countHiddenInfoboxes();
	}

	@Override
	public boolean render()
	{
		return plugin.allInfoboxesAreHidden();
	}
}
