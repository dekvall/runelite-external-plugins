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
    private static final int ICON_WIDTH = 30;

    Map<String, JLabel> labels = new HashMap<>();

    TableRow(HiscoreSkill skill)
    {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2, 0, 2, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel dataPanel = new JPanel(new GridLayout());
        dataPanel.setOpaque(false);

        String directory;
        if (skill.getType() == HiscoreSkillType.BOSS)
        {
            directory = "bosses/";

            dataPanel.add(createCell("kills"));
            dataPanel.add(createCell("rank"));
            dataPanel.add(createCell("ehb"));
        }
        else if (skill.getType() == HiscoreSkillType.ACTIVITY)
        {
            directory = "activities/";

            dataPanel.add(createCell("score"));
            dataPanel.add(createCell("rank"));
        }
        else
        {
            directory = "/skill_icons_small/";

            dataPanel.add(createCell("experience"));
            dataPanel.add(createCell("level"));
            dataPanel.add(createCell("rank"));
            dataPanel.add(createCell("ehp"));
        }

        String iconDirectory = directory + skill.name().toLowerCase() + ".png";
        log.debug("Loading icon for {}", iconDirectory);

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        JLabel iconLabel = new JLabel("", SwingConstants.CENTER);
        iconPanel.add(iconLabel);

        ImageIcon icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), iconDirectory));
        iconPanel.setPreferredSize(new Dimension(ICON_WIDTH, icon.getIconHeight()));

        iconLabel.setIcon(icon);
        iconLabel.setToolTipText(skill.getName());

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

        JLabel experienceLbl = labels.get("experience");
        experienceLbl.setText(Utils.formatNumber(experience));
        experienceLbl.setToolTipText(QuantityFormatter.formatNumber(experience));

        JLabel levelLbl = labels.get("level");
        int level = Experience.getLevelForXp((int) experience);
        int levelToDisplay = !virtualLevels && level > Experience.MAX_REAL_LEVEL ? Experience.MAX_REAL_LEVEL : level;
        levelLbl.setText(String.valueOf(levelToDisplay));
        levelLbl.setToolTipText(String.valueOf(levelToDisplay));

        JLabel rankLbl = labels.get("rank");
        rankLbl.setText(ranked ? Utils.formatNumber(rank) : "--");
        rankLbl.setToolTipText(ranked ? QuantityFormatter.formatNumber(rank) : "Unranked");

        JLabel ehpLbl = labels.get("ehp");
        ehpLbl.setText(Utils.formatNumber(ehp));
        ehpLbl.setToolTipText(QuantityFormatter.formatNumber(ehp));
    }

    void update(Boss boss, HiscoreSkill b)
    {
        int kills = boss.getKills();
        int minimumKc = Utils.getMinimumKc(b.name().toLowerCase());
        boolean ranked = kills >= minimumKc;

        int rank = boss.getRank();
        double ehb = boss.getEhb();

        JLabel killsLbl = labels.get("kills");
        killsLbl.setText(ranked ? Utils.formatNumber(kills) : "< " + minimumKc);
        killsLbl.setToolTipText(ranked ? QuantityFormatter.formatNumber(kills) : "The Hiscores only start tracking " + b.getName() + " after " + minimumKc + " kc");

        JLabel rankLbl = labels.get("rank");
        rankLbl.setText(ranked ? Utils.formatNumber(rank) : "--");
        rankLbl.setToolTipText(ranked ? QuantityFormatter.formatNumber(rank) : "Unranked");

        JLabel ehbLbl = labels.get("ehb");
        ehbLbl.setText(Utils.formatNumber(ehb));
        ehbLbl.setToolTipText(QuantityFormatter.formatNumber(ehb));
    }

    void update(Minigame minigame)
    {
        int score = minigame.getScore();
        int rank = minigame.getRank();
        boolean ranked = rank != -1;

        JLabel killsLbl = labels.get("score");
        killsLbl.setText(ranked ? Utils.formatNumber(score) : "--");
        killsLbl.setToolTipText(ranked ? QuantityFormatter.formatNumber(score) : "");

        JLabel rankLbl = labels.get("rank");
        rankLbl.setText(ranked ? Utils.formatNumber(rank) : "--");
        rankLbl.setToolTipText(ranked ? QuantityFormatter.formatNumber(rank) : "Unranked");
    }
}
