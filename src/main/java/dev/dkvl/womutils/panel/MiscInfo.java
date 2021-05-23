package dev.dkvl.womutils.panel;

import com.google.common.base.Strings;
import dev.dkvl.womutils.WomUtilsPlugin;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;

@Getter
public enum MiscInfo
{
    BUILD("Build", "build.png"),
    COUNTRY("Country", "flags_square/default.png"),
    EHP("EHP", "ehp.png"),
	EHB("EHB", "bosses/ehb.png"),
	EXP("Exp", "overall.png"),
	TTM("TTM", "ttm.png"),
    LAST_UPDATED("Last updated", null, "Last updated --");

    private final String hoverText;
    private final ImageIcon icon;
    private final String defaultText;

    MiscInfo(String hoverText, String iconPath, String defaultText)
	{
		this.hoverText = hoverText;
		this.icon = !Strings.isNullOrEmpty(iconPath) ? new ImageIcon(ImageUtil.loadImageResource(WomUtilsPlugin.class, iconPath)) : null;
		this.defaultText = !Strings.isNullOrEmpty(defaultText) ? defaultText : "--";
	}

    MiscInfo(String hoverText, String iconPath)
    {
    	this(hoverText, iconPath, null);
    }
}