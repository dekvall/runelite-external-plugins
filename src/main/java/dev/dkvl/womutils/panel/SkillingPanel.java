package dev.dkvl.womutils.panel;

import com.google.common.collect.ImmutableList;
import dev.dkvl.womutils.util.Format;
import dev.dkvl.womutils.WomUtilsConfig;
import dev.dkvl.womutils.beans.PlayerInfo;
import dev.dkvl.womutils.beans.Skill;
import dev.dkvl.womutils.beans.Snapshot;
import net.runelite.api.Experience;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.runelite.client.hiscore.HiscoreSkill.*;

class SkillingPanel extends JPanel
{
    /**
     * Real skills, ordered in the way they should be displayed in the panel.
     */
    private static final List<HiscoreSkill> SKILLS = ImmutableList.of(
        ATTACK, DEFENCE, STRENGTH,
        HITPOINTS, RANGED, PRAYER,
        MAGIC, COOKING, WOODCUTTING,
        FLETCHING, FISHING, FIREMAKING,
        CRAFTING, SMITHING, MINING,
        HERBLORE, AGILITY, THIEVING,
        SLAYER, FARMING, RUNECRAFT,
        HUNTER, CONSTRUCTION
    );
    static Color[] ROW_COLORS = {ColorScheme.DARKER_GRAY_COLOR, new Color(34, 34, 34)};

    TableRow overallRow;
    List<RowPair> tableRows = new ArrayList<>();

    WomUtilsConfig config;

    @Inject
    private SkillingPanel(WomUtilsConfig config)
    {
        this.config = config;

        setLayout(new GridLayout(0, 1));

        StatsTableHeader tableHeader = new StatsTableHeader("skilling");

        // Handle overall separately because it's special
        overallRow = new TableRow(
            OVERALL.name(), OVERALL.getName(), HiscoreSkillType.SKILL,
            "experience", "level", "rank", "ehp"
        );
        overallRow.setBackground(ROW_COLORS[1]);

        add(tableHeader);
        add(overallRow);

        for (int i = 0; i < SKILLS.size(); i++)
        {
            HiscoreSkill skill = SKILLS.get(i);
            TableRow row = new TableRow(
                skill.name(), skill.getName(), HiscoreSkillType.SKILL,
                "experience", "level", "rank", "ehp"
            );
            row.setBackground(ROW_COLORS[i%2]);

            tableRows.add(new RowPair(skill, row));
            add(row);
        }
    }

    public void update(PlayerInfo info)
    {
        if (info == null)
        {
            return;
        }

        Snapshot latestSnapshot = info.getLatestSnapshot();

        for (RowPair rp : tableRows)
        {
            HiscoreSkill skill = rp.getSkill();
            TableRow row = rp.getRow();

            row.update(latestSnapshot.getData().getSkills().getSkill(skill), config.virtualLevels());
        }

        updateTotalLevel(latestSnapshot);
    }

    private void updateTotalLevel(Snapshot snapshot)
    {
        int totalLevel = 0;
        Skill overall = snapshot.getData().getSkills().getSkill(OVERALL);
        long overallExperience = overall.getExperience();
        int overallRank = overall.getRank();

        for (HiscoreSkill skill : SKILLS)
        {
            int experience = (int) snapshot.getData().getSkills().getSkill(skill).getExperience();
            int level = experience >= 0 ? Experience.getLevelForXp(experience) : 0;
            totalLevel += !config.virtualLevels() && level > Experience.MAX_REAL_LEVEL ? Experience.MAX_REAL_LEVEL : level;
        }

        JLabel expLabel = overallRow.labels.get("experience");
        JLabel levelLabel = overallRow.labels.get("level");
        JLabel rankLabel = overallRow.labels.get("rank");
        JLabel ehpLabel = overallRow.labels.get("ehp");

        expLabel.setText(overallExperience >= 0 ? Format.formatNumber(overallExperience) : "--");
        expLabel.setToolTipText(overallExperience >= 0 ? QuantityFormatter.formatNumber(overallExperience) : "");

        levelLabel.setText(totalLevel > 0 ? String.valueOf(totalLevel) : "--");
        levelLabel.setToolTipText(totalLevel > 0 ? QuantityFormatter.formatNumber(totalLevel) : "");

        rankLabel.setText(overallRank > 0 ? Format.formatNumber(overallRank) : "--");
        rankLabel.setToolTipText(overallRank > 0 ? QuantityFormatter.formatNumber(overallRank) : "Unranked");

        ehpLabel.setText(Format.formatNumber(overall.getEhp()));
        ehpLabel.setToolTipText(QuantityFormatter.formatNumber(overall.getEhp()));
    }

    public void reset()
    {
        for (Map.Entry<String, JLabel> entry : overallRow.labels.entrySet())
        {
            JLabel label = entry.getValue();
            label.setText("--");
            label.setToolTipText("");
        }

        for (RowPair rp : tableRows)
        {
            TableRow row = rp.getRow();

            for (Map.Entry<String, JLabel> e : row.labels.entrySet())
            {
                JLabel label = e.getValue();
                label.setText("--");
                label.setToolTipText("");
            }
        }
    }
}
