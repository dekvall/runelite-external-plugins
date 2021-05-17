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
package dev.dkvl.womutils.panel;

import dev.dkvl.womutils.Format;
import dev.dkvl.womutils.Utils;
import dev.dkvl.womutils.beans.Boss;
import dev.dkvl.womutils.beans.Minigame;
import dev.dkvl.womutils.beans.Skill;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.hiscore.HiscoreSkill;
import net.runelite.http.api.hiscore.HiscoreSkillType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TableRow extends JPanel
{
    private static final int ICON_WIDTH = 35;

    Map<String, JLabel> labels = new HashMap<>();

    TableRow(String name, String formattedName, HiscoreSkillType type, String... labels)
    {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2, 0, 2, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel dataPanel = new JPanel(new GridLayout());
        dataPanel.setOpaque(false);

        final String directory;

        if (type == HiscoreSkillType.BOSS)
        {
            directory = "../bosses/";
        }
        else if (type == HiscoreSkillType.ACTIVITY)
        {
            directory = "../activities/";
        }
        else
        {
            directory = "/skill_icons_small/";
        }

        for (String l : labels)
        {
            dataPanel.add(createCell(l));
        }

        String iconDirectory = directory + name.toLowerCase() + ".png";
        log.debug("Loading icon for {}", iconDirectory);

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        JLabel iconLabel = new JLabel("", SwingConstants.CENTER);
        iconPanel.add(iconLabel);

        ImageIcon icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), iconDirectory));
        iconPanel.setPreferredSize(new Dimension(ICON_WIDTH, icon.getIconHeight()));

        iconLabel.setIcon(icon);
        iconLabel.setToolTipText(formattedName);

        add(iconPanel, BorderLayout.WEST);
        add(dataPanel);
    }

    private JLabel createCell(String l)
    {
        JLabel label = new JLabel("--", SwingConstants.CENTER);
        label.setFont(FontManager.getRunescapeSmallFont());

        labels.put(l, label);

        return label;
    }

    void update(Skill skill, boolean virtualLevels)
    {
        long experience = skill.getExperience();
        int rank = skill.getRank();
        boolean ranked = rank != -1;
        double ehp = skill.getEhp();

        JLabel experienceLabel = labels.get("experience");
        experienceLabel.setText(Format.formatNumber(experience));
        experienceLabel.setToolTipText(QuantityFormatter.formatNumber(experience));

        JLabel levelLabel = labels.get("level");
        int level = Experience.getLevelForXp((int) experience);
        int levelToDisplay = !virtualLevels && level > Experience.MAX_REAL_LEVEL ? Experience.MAX_REAL_LEVEL : level;
        levelLabel.setText(String.valueOf(levelToDisplay));
        levelLabel.setToolTipText(String.valueOf(levelToDisplay));

        JLabel rankLabel = labels.get("rank");
        rankLabel.setText(ranked ? Format.formatNumber(rank) : "--");
        rankLabel.setToolTipText(ranked ? QuantityFormatter.formatNumber(rank) : "Unranked");

        JLabel ehpLabel = labels.get("ehp");
        ehpLabel.setText(Format.formatNumber(ehp));
        ehpLabel.setToolTipText(QuantityFormatter.formatNumber(ehp));
    }

    void update(Boss boss, HiscoreSkill b)
    {
        int kills = boss.getKills();
        int minimumKc = Utils.getMinimumKc(b);
        boolean ranked = kills >= minimumKc;

        int rank = boss.getRank();
        double ehb = boss.getEhb();

        JLabel killsLabel = labels.get("kills");
        killsLabel.setText(ranked ? Format.formatNumber(kills) : "< " + minimumKc);
        killsLabel.setToolTipText(ranked ? QuantityFormatter.formatNumber(kills) : "The Hiscores only start tracking " + b.getName() + " after " + minimumKc + " kc");

        JLabel rankLabel = labels.get("rank");
        rankLabel.setText(ranked ? Format.formatNumber(rank) : "--");
        rankLabel.setToolTipText(ranked ? QuantityFormatter.formatNumber(rank) : "Unranked");

        JLabel ehbLabel = labels.get("ehb");
        ehbLabel.setText(Format.formatNumber(ehb));
        ehbLabel.setToolTipText(QuantityFormatter.formatNumber(ehb));
    }

    void update(Minigame minigame)
    {
        int score = minigame.getScore();
        int rank = minigame.getRank();
        boolean ranked = rank != -1;

        JLabel killsLabel = labels.get("score");
        killsLabel.setText(ranked ? Format.formatNumber(score) : "--");
        killsLabel.setToolTipText(ranked ? QuantityFormatter.formatNumber(score) : "");

        JLabel rankLabel = labels.get("rank");
        rankLabel.setText(ranked ? Format.formatNumber(rank) : "--");
        rankLabel.setToolTipText(ranked ? QuantityFormatter.formatNumber(rank) : "Unranked");
    }
}
