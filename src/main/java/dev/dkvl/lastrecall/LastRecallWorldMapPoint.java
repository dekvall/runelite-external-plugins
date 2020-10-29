package dev.dkvl.lastrecall;

import java.awt.image.BufferedImage;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.util.ImageUtil;

class LastRecallWorldMapPoint extends WorldMapPoint
{
	private final BufferedImage image;
	private final BufferedImage arrowIcon;
	private final Point point;

	LastRecallWorldMapPoint(final WorldPoint worldPoint)
	{
		super(worldPoint, null);

		image = ImageUtil.getResourceStreamFromClass(getClass(), "map-icon.png");
		arrowIcon = ImageUtil.getResourceStreamFromClass(getClass(), "arrow-icon.png");
		point = new Point(arrowIcon.getWidth() / 2, arrowIcon.getHeight());

		this.setSnapToEdge(true);
		this.setJumpOnClick(true);
		this.setImage(arrowIcon);
		this.setImagePoint(point);
	}

	@Override
	public void onEdgeSnap()
	{
		this.setImage(image);
		this.setImagePoint(null);
	}

	@Override
	public void onEdgeUnsnap()
	{
		this.setImage(arrowIcon);
		this.setImagePoint(point);
	}
}