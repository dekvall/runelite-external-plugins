package dev.dkvl.womutils.panel;

import com.google.common.collect.ImmutableList;
import dev.dkvl.womutils.beans.PlayerInfo;
import dev.dkvl.womutils.beans.Snapshot;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.runelite.client.hiscore.HiscoreSkill.*;

public class ActivitiesPanel extends JPanel
{
    /**
     * Activities, ordered in the way they should be displayed in the panel
     */
    private static final List<HiscoreSkill> ACTIVITIES = ImmutableList.of(
        LEAGUE_POINTS, BOUNTY_HUNTER_HUNTER, BOUNTY_HUNTER_ROGUE,
        CLUE_SCROLL_ALL, CLUE_SCROLL_BEGINNER, CLUE_SCROLL_EASY,
        CLUE_SCROLL_MEDIUM, CLUE_SCROLL_HARD, CLUE_SCROLL_ELITE,
        CLUE_SCROLL_MASTER, LAST_MAN_STANDING, PVP_ARENA_RANK,
        SOUL_WARS_ZEAL, RIFTS_CLOSED
    );
    static Color[] ROW_COLORS = {ColorScheme.DARKER_GRAY_COLOR, new Color(34, 34, 34)};

    List<RowPair> tableRows = new ArrayList<>();

    @Inject
    private ActivitiesPanel()
    {
        setLayout(new GridLayout(0, 1));

        StatsTableHeader tableHeader = new StatsTableHeader("activities");

        add(tableHeader);

        for (int i = 0; i < ACTIVITIES.size(); i++)
        {
            HiscoreSkill activity = ACTIVITIES.get(i);
            TableRow row = new TableRow(
                activity.name(), activity.getName(), HiscoreSkillType.ACTIVITY,
                "score", "rank"
            );
            row.setBackground(ROW_COLORS[1-i%2]);

            tableRows.add(new RowPair(activity, row));
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
            HiscoreSkill minigame = rp.getSkill();
            TableRow row = rp.getRow();

            row.update(latestSnapshot.getMinigame(minigame));
        }
    }

    public void reset()
    {
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
