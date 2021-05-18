package dev.dkvl.womutils.panel;

import dev.dkvl.womutils.CountryIcon;
import dev.dkvl.womutils.Format;
import dev.dkvl.womutils.beans.PlayerInfo;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MiscInfoLabel extends JLabel
{
    MiscInfo info;

    public MiscInfoLabel(MiscInfo info)
    {
        this.info = info;

        setFont(FontManager.getRunescapeSmallFont());
        setBorder(new EmptyBorder(5, 10, 5, 5));
        setText(info.getClearedString());
        setToolTipText(info.getRawString());

        if (info != MiscInfo.LAST_UPDATED)
        {
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
            setOpaque(true);
            setIcon(new ImageIcon(ImageUtil.loadImageResource(getClass(), info.getIconPath())));
        }
        else
        {
            setHorizontalAlignment(JLabel.CENTER);
        }
    }

    public void format(PlayerInfo result, boolean relative)
    {
        switch (info)
        {
            case COUNTRY:
                String country = result.getCountry();
                String countryTxt = country == null ? "--" : country;
                String languageCode = country == null ? "default" : country.toLowerCase();
                setIcon(CountryIcon.loadSquareImage(languageCode));
                setText(countryTxt);
                break;
            case BUILD:
                setText("" + result.getBuild());
                break;
            case TTM:
                setText(Format.formatNumber(result.getTtm()) + 'h');
                break;
            case EHP:
                setText(Format.formatNumber(result.getEhp()));
                break;
            case EHB:
                setText(Format.formatNumber(result.getEhb()));
                break;
            case EXP:
                setText(Format.formatNumber(result.getExp()));
                break;
            case LAST_UPDATED:
                setText(info.getRawString() + " " + Format.formatDate(result.getUpdatedAt(), relative));
                break;
        }
    }

    public void reset()
    {
        if (info == MiscInfo.COUNTRY)
        {
            setIcon(new ImageIcon(ImageUtil.loadImageResource(getClass(), info.getIconPath())));
        }
        setText(info.getClearedString());
    }
}