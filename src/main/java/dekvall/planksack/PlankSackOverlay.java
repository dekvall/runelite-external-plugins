package dekvall.planksack;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.ItemID;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class PlankSackOverlay extends WidgetItemOverlay
{
	private final PlankSackPlugin plugin;

	@Inject
	PlankSackOverlay(PlankSackPlugin plugin)
	{
		this.plugin = plugin;
		showOnInventory();
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		if (itemId != ItemID.PLANK_SACK)
		{
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		Point location = itemWidget.getCanvasLocation();
		Plank[] planks = Plank.values();
		for (int i = 0; i < planks.length; i++)
		{
			int amount = plugin.getCount().get(planks[i]);

			graphics.setColor(Color.black);
			graphics.drawString("" + amount, location.getX() + 5,
				location.getY() + 13 + (graphics.getFontMetrics().getHeight() - 1) * i);

			graphics.setColor(Color.YELLOW);
			graphics.drawString("" + amount, location.getX() + 4,
				location.getY() + 12 + (graphics.getFontMetrics().getHeight() - 1) * i);

		}



	}
}
