package dekvall.bankdiff;

import com.google.inject.Inject;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import java.awt.*;

public class NegativeOneItemOverlay extends WidgetItemOverlay {

    private final BankDiffPlugin plugin;

    @Inject
    NegativeOneItemOverlay(BankDiffPlugin plugin)
    {
        this.plugin = plugin;
        showOnBank();
    }

    @Override
    public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem itemWidget) {
        if (!plugin.isDiffViewToggled()
                || !plugin.getNegativeOneCounts().contains(itemId)
                || itemWidget.getWidget().getParentId() != WidgetInfo.BANK_ITEM_CONTAINER.getId())
        {
            return;
        }

        g.setFont(FontManager.getRunescapeSmallFont());
        Point location = itemWidget.getCanvasLocation();

        g.setColor(Color.BLACK);
        g.drawString("-1", location.getX() + 1, location.getY() + 11);

        g.setColor(Color.YELLOW);
        g.drawString("-1", location.getX(), location.getY() + 10);
    }
}
