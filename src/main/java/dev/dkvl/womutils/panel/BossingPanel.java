package dev.dkvl.womutils.panel;

import com.google.common.collect.ImmutableList;
import dev.dkvl.womutils.Format;
import dev.dkvl.womutils.beans.PlayerInfo;
import dev.dkvl.womutils.beans.Snapshot;
import dev.dkvl.womutils.beans.VirtualSkill;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.hiscore.HiscoreSkill;
import net.runelite.http.api.hiscore.HiscoreSkillType;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.runelite.http.api.hiscore.HiscoreSkill.*;

class BossingPanel extends JPanel
{
    /**
     * Bosses, ordered in the way they should be displayed in the panel.
     */
    private static final List<HiscoreSkill> BOSSES = ImmutableList.of(
        ABYSSAL_SIRE, ALCHEMICAL_HYDRA, BARROWS_CHESTS,
        BRYOPHYTA, CALLISTO, CERBERUS,
        CHAMBERS_OF_XERIC, CHAMBERS_OF_XERIC_CHALLENGE_MODE, CHAOS_ELEMENTAL,
        CHAOS_FANATIC, COMMANDER_ZILYANA, CORPOREAL_BEAST,
        DAGANNOTH_PRIME, DAGANNOTH_REX, DAGANNOTH_SUPREME,
        CRAZY_ARCHAEOLOGIST, DERANGED_ARCHAEOLOGIST, GENERAL_GRAARDOR,
        GIANT_MOLE, GROTESQUE_GUARDIANS, HESPORI,
        KALPHITE_QUEEN, KING_BLACK_DRAGON, KRAKEN,
        KREEARRA, KRIL_TSUTSAROTH, MIMIC,
        NIGHTMARE, OBOR, SARACHNIS,
        SCORPIA, SKOTIZO, TEMPOROSS, THE_GAUNTLET,
        THE_CORRUPTED_GAUNTLET, THEATRE_OF_BLOOD, THERMONUCLEAR_SMOKE_DEVIL,
        TZKAL_ZUK, TZTOK_JAD, VENENATIS,
        VETION, VORKATH, WINTERTODT,
        ZALCANO, ZULRAH
    );

    TableRow totalEhbRow;
    List<RowPair> tableRows = new ArrayList<>();

    @Inject
    private BossingPanel()
    {
        setLayout(new GridLayout(0, 1));

        StatsTableHeader tableHeader = new StatsTableHeader("bossing");

        // Handle total ehb row separately because it's special
        totalEhbRow = new TableRow(
            "ehb", "EHB", HiscoreSkillType.BOSS,
            "kills", "rank", "ehb"
        );

        add(tableHeader);
        add(totalEhbRow);

        for (HiscoreSkill boss : BOSSES)
        {
            TableRow row = new TableRow(
                boss.name(), boss.getName(), HiscoreSkillType.BOSS,
                "kills", "rank", "ehb"
            );

            tableRows.add(new RowPair(boss, row));
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
            HiscoreSkill boss = rp.getSkill();
            TableRow row = rp.getRow();

            row.update(latestSnapshot.getBoss(boss), boss);
        }

        updateTotalEhb(latestSnapshot.getEhb());
    }

    private void updateTotalEhb(VirtualSkill ehb)
    {
        JLabel rankLabel = totalEhbRow.labels.get("rank");
        JLabel ehbLabel = totalEhbRow.labels.get("ehb");

        int rank = ehb.getRank();
        double value = ehb.getValue();

        rankLabel.setText(Format.formatNumber(rank));
        rankLabel.setToolTipText(QuantityFormatter.formatNumber(rank));

        ehbLabel.setText(Format.formatNumber(value));
        ehbLabel.setToolTipText(QuantityFormatter.formatNumber(value));
    }

    public void reset()
    {
        for (Map.Entry<String, JLabel> entry : totalEhbRow.labels.entrySet())
        {
            entry.getValue().setText("--");
        }

        for (RowPair rp : tableRows)
        {
            TableRow row = rp.getRow();

            for (Map.Entry<String, JLabel> e : row.labels.entrySet())
            {
                e.getValue().setText("--");
            }
        }
    }
}
