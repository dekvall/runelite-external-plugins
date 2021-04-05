/*
 * Copyright (c) 2021, Rorro <https://github.com/rorro>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.dkvl.womutils;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class StatsTableHeader extends JPanel
{
    private static final String[] SKILLING_LABELS = {"Exp", "Level", "Rank", "EHP"};
    private static final String[] BOSSING_LABELS = {"Kills", "Rank", "EHB"};
    private static final String[] ACTIVITIES_LABELS = {"Score", "Rank"};

    private static final Map<String, String[]> HEADER_LABELS = new HashMap<String, String[]>()
    {{
        put("skilling", SKILLING_LABELS);
        put("bossing", BOSSING_LABELS);
        put("activities", ACTIVITIES_LABELS);
    }};

    StatsTableHeader(String stats)
    {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2, 0, 2, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel iconPnl = new JPanel(new BorderLayout());
        iconPnl.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        iconPnl.setPreferredSize(new Dimension(30, 0));
        iconPnl.add(new JLabel("", SwingConstants.CENTER));

        JPanel headersPanel = new JPanel(new GridLayout());
        headersPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        for (String label : HEADER_LABELS.get(stats))
        {
            JLabel lbl = new JLabel(label, SwingConstants.CENTER);
            lbl.setFont(FontManager.getRunescapeSmallFont());
            headersPanel.add(lbl);
        }

        add(iconPnl, BorderLayout.WEST);
        add(headersPanel);
    }
}
