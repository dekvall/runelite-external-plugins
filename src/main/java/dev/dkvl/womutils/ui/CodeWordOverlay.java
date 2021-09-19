package dev.dkvl.womutils.ui;

import com.google.common.base.Strings;
import dev.dkvl.womutils.WomUtilsConfig;
import dev.dkvl.womutils.WomUtilsPlugin;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class CodeWordOverlay extends OverlayPanel
{
	private final WomUtilsConfig config;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'")
		.withZone(ZoneOffset.UTC);

	@Inject
	private CodeWordOverlay(WomUtilsPlugin plugin, WomUtilsConfig config)
	{
		super(plugin);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setPriority(OverlayPriority.LOW);
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if(!config.displayCodeword() || Strings.isNullOrEmpty(config.configuredCodeword()))
		{
			return null;
		}
		if(config.showTimestamp())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(config.configuredCodeword())
				.leftColor(config.codewordColor())
				.right(FORMATTER.format(Instant.now()))
				.rightColor(config.timestampColor())
				.build());
		}
		else
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text(config.configuredCodeword())
				.color(config.codewordColor())
				.build());
		}
		return super.render(graphics);
	}
}
