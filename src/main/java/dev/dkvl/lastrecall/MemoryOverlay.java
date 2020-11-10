package dev.dkvl.lastrecall;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

public class MemoryOverlay extends WidgetItemOverlay
{
	private final Client client;
	private final LastRecallConfig config;
	private final TooltipManager tooltipManager;
	private final SpriteManager spriteManager;

	@Inject
	MemoryOverlay(Client client, LastRecallConfig config, TooltipManager tooltipManager, SpriteManager spriteManager)
	{
		this.client = client;
		this.config = config;
		this.tooltipManager = tooltipManager;
		this.spriteManager = spriteManager;
		showOnBank();
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		if (itemId != ItemID.CRYSTAL_OF_MEMORIES || config.region() == null)
		{
			return;
		}
		Point location = itemWidget.getCanvasLocation();
		int spriteId = RegionShield.getRegionShield(config.region());
		BufferedImage shield = spriteManager.getSprite(spriteId, 0);

		OverlayUtil.renderImageLocation(graphics, new Point(location.getX() + 18, location.getY() - 1), shield);

		NamedRegion namedRegion = NamedRegion.fromWorldPoint(config.location());
		String loc = namedRegion != null ? namedRegion.getName() : config.region();
		String tooltip = "Stores a teleport to " + ColorUtil.wrapWithColorTag(loc, JagexColors.MENU_TARGET) + ".";

		if (itemWidget.getCanvasBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			tooltipManager.add(new Tooltip(tooltip));
		}
	}
}
