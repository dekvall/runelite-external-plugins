package dev.dkvl.womutils.panel;

import lombok.Getter;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;

@Getter
public enum MiscInfo
{
    BUILD("Build", new ImageIcon(ImageUtil.loadImageResource(MiscInfo.class, "../build.png")), "--"),
    COUNTRY("Country", new ImageIcon(ImageUtil.loadImageResource(MiscInfo.class, "../flags_square/default.png")), "--"),
    TTM("TTM", new ImageIcon(ImageUtil.loadImageResource(MiscInfo.class, "../ttm.png")), "--"),
    EHP("EHP", new ImageIcon(ImageUtil.loadImageResource(MiscInfo.class, "../ehp.png")), "--"),
    EHB("EHB", new ImageIcon(ImageUtil.loadImageResource(MiscInfo.class, "../bosses/ehb.png")), "--"),
    EXP("Exp", new ImageIcon(ImageUtil.loadImageResource(MiscInfo.class, "../overall.png")), "--"),
    LAST_UPDATED("Last updated", "Last updated --");

    private final String hoverText;
    private final ImageIcon icon;
    private final String defaultText;

    MiscInfo(String hoverText, String defaultText)
    {
        this.hoverText = hoverText;
        this.defaultText = defaultText;
        icon = null;
    }

    MiscInfo(String hoverText, ImageIcon icon, String defaultText)
    {
        this.hoverText = hoverText;
        this.defaultText = defaultText;
        this.icon = icon;
    }
}