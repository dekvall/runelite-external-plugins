/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * Copyright (c) 2019, Bram91 <https://github.com/bram91>
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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import dev.dkvl.womutils.*;
import dev.dkvl.womutils.beans.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Player;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.hiscore.HiscoreEndpoint;
import net.runelite.http.api.hiscore.HiscoreSkill;
import static net.runelite.http.api.hiscore.HiscoreSkill.*;
import net.runelite.http.api.hiscore.HiscoreSkillType;
import okhttp3.HttpUrl;

@Slf4j
public class WomPanel extends PluginPanel
{
	private WomUtilsConfig config;

	/* The maximum allowed username length in RuneScape accounts */
	private static final int MAX_USERNAME_LENGTH = 12;

	/**
	 * Real skills, ordered in the way they should be displayed in the panel.
	 */
	private static final List<HiscoreSkill> SKILLS = ImmutableList.of(
		ATTACK, HITPOINTS, MINING,
		STRENGTH, AGILITY, SMITHING,
		DEFENCE, HERBLORE, FISHING,
		RANGED, THIEVING, COOKING,
		PRAYER, CRAFTING, FIREMAKING,
		MAGIC, FLETCHING, WOODCUTTING,
		RUNECRAFT, SLAYER, FARMING,
		CONSTRUCTION, HUNTER
	);

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

	/**
	 * Activities, ordered in the way they should be displayed in the panel
	 */
	private static final List<HiscoreSkill> ACTIVITIES = ImmutableList.of(
		LEAGUE_POINTS, BOUNTY_HUNTER_HUNTER, BOUNTY_HUNTER_ROGUE,
		CLUE_SCROLL_ALL, CLUE_SCROLL_BEGINNER, CLUE_SCROLL_EASY,
		CLUE_SCROLL_MEDIUM, CLUE_SCROLL_HARD, CLUE_SCROLL_ELITE,
		CLUE_SCROLL_MASTER, LAST_MAN_STANDING, SOUL_WARS_ZEAL
	);

	private final Client client;
	private final NameAutocompleter nameAutocompleter;
	private final WomClient womClient;

	private final IconTextField searchBar;

	private final List<InfoLabel> miscInfoLabels = new ArrayList<>();
	private final List<JButton> buttons = new ArrayList<>();
	private final List<RowPair> tableRows = new ArrayList<>();

	TableRow overallRow;
	TableRow totalEhbRow;

	/* The currently selected endpoint */
	private HiscoreEndpoint selectedEndPoint;

	/* Used to prevent users from switching endpoint tabs while the results are loading */
	private boolean loading = false;

	@Inject
	public WomPanel(Client client, NameAutocompleter nameAutocompleter, WomClient womClient, WomUtilsConfig config)
	{
		this.client = client;
		this.nameAutocompleter = nameAutocompleter;
		this.womClient = womClient;
		this.config = config;

		// The layout seems to be ignoring the top margin and only gives it
		// a 2-3 pixel margin, so I set the value to 18 to compensate
		// TODO: Figure out why this layout is ignoring most of the top margin
		setBorder(new EmptyBorder(18, 10, 0, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new GridBagLayout());

		// Expand sub items to fit width of panel, align to top of panel
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets(0, 0, 10, 0);

		searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 30));
		searchBar.addActionListener(e -> lookup());
		searchBar.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() != 2)
				{
					return;
				}
				if (client == null)
				{
					return;
				}

				Player localPlayer = client.getLocalPlayer();

				if (localPlayer != null)
				{
					lookup(localPlayer.getName());
				}
			}
		});
		searchBar.addClearListener(() ->
		{
			searchBar.setIcon(IconTextField.Icon.SEARCH);
			searchBar.setEditable(true);
			loading = false;
		});

		add(searchBar, c);

		// TODO: add an update button if the player isn't tracked

		c.gridy++;

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(1, 2, 7, 7));

		JButton updateBtn = new JButton();
		updateBtn.setFont(FontManager.getRunescapeSmallFont());
		updateBtn.setEnabled(false);
		updateBtn.addActionListener(e ->
			womClient.updatePlayer(sanitize(searchBar.getText())));
		updateBtn.setText("Update");

		JButton profileBtn = new JButton();
		profileBtn.setFont(FontManager.getRunescapeSmallFont());
		profileBtn.setEnabled(false);
		profileBtn.addActionListener(e ->
			openPlayerProfile(sanitize(searchBar.getText())));
		profileBtn.setText("Open Profile");

		buttons.add(updateBtn);
		buttons.add(profileBtn);

		buttonsPanel.add(updateBtn);
		buttonsPanel.add(profileBtn);

		add(buttonsPanel, c);

		c.gridy++;

		JLabel overviewTitle = new JLabel("Overview");
		overviewTitle.setFont(FontManager.getRunescapeBoldFont());
		add(overviewTitle, c);
		c.gridy++;

		JLabel lastUpdated = new JLabel("Last updated: --");

		JPanel miscInfoPanel = new JPanel();
		miscInfoPanel.setLayout(new GridLayout(3, 2, 5, 5));

		miscInfoLabels.add(new InfoLabel("Build", new JLabel("Build: --")));
		miscInfoLabels.add(new InfoLabel("Country", new JLabel("Country: --")));
		miscInfoLabels.add(new InfoLabel("TTM", new JLabel("TTM: --")));
		miscInfoLabels.add(new InfoLabel("EHP", new JLabel("EHP: --")));
		miscInfoLabels.add(new InfoLabel("EHB", new JLabel("EHB: --")));
		miscInfoLabels.add(new InfoLabel("Exp", new JLabel("Exp: --")));
		miscInfoLabels.add(new InfoLabel("Last updated", lastUpdated));

		for (InfoLabel infoLabel : miscInfoLabels) {
			JLabel label = infoLabel.getLabel();

			if (infoLabel.getRawString().equalsIgnoreCase("last updated"))
			{
				label.setFont(FontManager.getRunescapeSmallFont());
				label.setHorizontalAlignment(JLabel.CENTER);
				continue;
			}

			label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			label.setOpaque(true);
			label.setFont(FontManager.getRunescapeFont());

			miscInfoPanel.add(label);
		}

		add(miscInfoPanel, c);
		c.gridy++;

		add(lastUpdated, c);
		c.gridy++;

		JLabel statsTitle = new JLabel("Stats");
		statsTitle.setFont(FontManager.getRunescapeBoldFont());

		add(statsTitle, c);
		c.gridy++;

		StatsTableHeader skillingSth = new StatsTableHeader("skilling");
		StatsTableHeader bossingSth = new StatsTableHeader("bossing");
		StatsTableHeader activitiesSth = new StatsTableHeader("activities");

		JPanel skillPanel = new JPanel(new GridLayout(0, 1));
		// Handle overall separately because it's special
		overallRow = new TableRow(
			OVERALL.name(), OVERALL.getName(), HiscoreSkillType.SKILL,
			"experience", "level", "rank", "ehp"
		);
		skillPanel.add(overallRow);

		for (HiscoreSkill skill : SKILLS)
		{
			TableRow row = new TableRow(
				skill.name(), skill.getName(), HiscoreSkillType.SKILL,
				"experience", "level", "rank", "ehp"
			);

			tableRows.add(new RowPair(skill, row));
			skillPanel.add(row);
		}

		JPanel bossPanel = new JPanel(new GridLayout(0, 1));
		// Handle total ehb row separately because it's special
		totalEhbRow = new TableRow(
			"ehb", "EHB", HiscoreSkillType.BOSS,
			"kills", "rank", "ehb"
		);
		bossPanel.add(totalEhbRow);

		for (HiscoreSkill boss : BOSSES)
		{
			TableRow row = new TableRow(
				boss.name(), boss.getName(), HiscoreSkillType.BOSS,
				"kills", "rank", "ehb"
			);

			tableRows.add(new RowPair(boss, row));
			bossPanel.add(row);
		}

		JPanel activitiesPanel = new JPanel(new GridLayout(0, 1));
		for (HiscoreSkill activity : ACTIVITIES)
		{
			TableRow row = new TableRow(
				activity.name(), activity.getName(), HiscoreSkillType.ACTIVITY,
				"score", "rank"
			);

			tableRows.add(new RowPair(activity, row));
			activitiesPanel.add(row);
		}

		addPanel(skillingSth, skillPanel, c);
		addPanel(bossingSth, bossPanel, c);
		addPanel(activitiesSth, activitiesPanel, c);

		addInputKeyListener(nameAutocompleter);
	}

	public void shutdown()
	{
		removeInputKeyListener(nameAutocompleter);
	}

	@Override
	public void onActivate()
	{
		super.onActivate();
		searchBar.requestFocusInWindow();
	}

	private void toggleButtons(boolean enabled)
	{
		for (JButton button : buttons)
		{
			button.setEnabled(enabled);
		}
	}

	public void lookup(String username)
	{
		searchBar.setText(username);
		lookup();
	}

	private void lookup()
	{
		final String lookup = sanitize(searchBar.getText());
		toggleButtons(false);

		if (Strings.isNullOrEmpty(lookup))
		{
			return;
		}

		/* RuneScape usernames can't be longer than 12 characters long */
		if (lookup.length() > MAX_USERNAME_LENGTH)
		{
			searchBar.setIcon(IconTextField.Icon.ERROR);
			loading = false;
			return;
		}

		searchBar.setEditable(false);
		searchBar.setIcon(IconTextField.Icon.LOADING_DARKER);
		loading = true;

		for (InfoLabel infoLabel : miscInfoLabels)
		{
			infoLabel.getLabel().setText(infoLabel.getRawString() + ": --");
		}

		for (Map.Entry<String, JLabel> entry : overallRow.labels.entrySet())
		{
			entry.getValue().setText("--");
		}

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

		// if for some reason no endpoint was selected, default to normal
		if (selectedEndPoint == null)
		{
			selectedEndPoint = HiscoreEndpoint.NORMAL;
		}

		womClient.lookupAsync(lookup).whenCompleteAsync((result, ex) ->
			SwingUtilities.invokeLater(() ->
			{
				if (!sanitize(searchBar.getText()).equals(lookup))
				{
					// search has changed in the meantime
					return;
				}

				if (result == null || ex != null)
				{
					if (ex != null)
					{
						log.warn("Error fetching Wise Old Man data " + ex.getMessage());
					}

					searchBar.setIcon(IconTextField.Icon.ERROR);
					searchBar.setEditable(true);
					loading = false;

					// Track option
					return;
				}

				if (result.getLatestSnapshot() == null)
				{
					log.warn("Player on WOM without snapshot {}.", lookup);
					searchBar.setIcon(IconTextField.Icon.ERROR);
					searchBar.setEditable(true);
					loading = false;

					// Update option
					return;
				}

				//successful player search
				searchBar.setIcon(IconTextField.Icon.SEARCH);
				searchBar.setEditable(true);
				loading = false;

				toggleButtons(true);
				applyResult(result);
			}));
	}

	private void applyOverviewResult(PlayerInfo result)
	{
		for (InfoLabel infoLabel : miscInfoLabels)
		{
			JLabel label = infoLabel.getLabel();
			switch (infoLabel.getRawString().toLowerCase())
			{
				case "country":
					String cntry = result.getCountry();
					String countryTxt = cntry == null ? "--" : cntry;
					label.setText("Country: " + countryTxt);
					break;
				case "build":
					label.setText("Build: " + result.getBuild().toString());
					break;
				case "ttm":
					label.setText("TTM: " + Format.formatNumber(result.getTtm()) + 'h');
					break;
				case "ehp":
					label.setText("EHP: " + Format.formatNumber(result.getEhp()));
					break;
				case "ehb":
					label.setText("EHB: " + Format.formatNumber(result.getEhb()));
					break;
				case "exp":
					label.setText("Exp: " + Format.formatNumber(result.getExp()));
					break;
				case "last updated":
					label.setText("Last updated " + Format.formatDate(result.getUpdatedAt(), config.relativeTime()));
					break;
			}
		}
	}

	private void applyStatsResult(PlayerInfo result)
	{
		if (result == null)
		{
			return;
		}

		Snapshot latestSnapshot = result.getLatestSnapshot();

		for (RowPair rp : tableRows)
		{
			HiscoreSkill skill = rp.getSkill();
			TableRow row = rp.getRow();

			if (skill.getType() == HiscoreSkillType.SKILL)
			{
				row.update(latestSnapshot.getSkill(skill), config.virtualLevels());
			}
			else if (skill.getType() == HiscoreSkillType.ACTIVITY)
			{
				row.update(latestSnapshot.getMinigame(skill));
			}
			else
			{
				row.update(latestSnapshot.getBoss(skill), skill);
			}
		}
		updateTotalLevel(latestSnapshot);
		updateTotalEhb(latestSnapshot.getEhb());
	}

	private void applyResult(PlayerInfo result)
	{
		assert SwingUtilities.isEventDispatchThread();

		nameAutocompleter.addToSearchHistory(result.getUsername());

		applyOverviewResult(result);
		applyStatsResult(result);

	}

	void addInputKeyListener(KeyListener l)
	{
		this.searchBar.addKeyListener(l);
	}

	void removeInputKeyListener(KeyListener l)
	{
		this.searchBar.removeKeyListener(l);
	}

	private static String sanitize(String lookup)
	{
		return lookup.replace('\u00A0', ' ');
	}

	private void addPanel(StatsTableHeader sth, JPanel panel, GridBagConstraints c)
	{
		c.insets.bottom = 0;
		add(sth, c);
		c.gridy++;
		c.insets.bottom = 10;
		add(panel, c);
		c.gridy++;
	}

	private void updateTotalLevel(Snapshot snapshot)
	{
		int totalLevel = 0;
		Skill overall = snapshot.getSkill(OVERALL);

		for (HiscoreSkill skill : SKILLS)
		{
			int level = Experience.getLevelForXp((int) snapshot.getSkill(skill).getExperience());
			totalLevel += !config.virtualLevels() && level > Experience.MAX_REAL_LEVEL ? Experience.MAX_REAL_LEVEL : level;
		}

		JLabel expLabel = overallRow.labels.get("experience");
		JLabel levelLabel = overallRow.labels.get("level");
		JLabel rankLabel = overallRow.labels.get("rank");
		JLabel ehpLabel = overallRow.labels.get("ehp");

		expLabel.setText(Format.formatNumber(overall.getExperience()));
		expLabel.setToolTipText(QuantityFormatter.formatNumber(overall.getExperience()));

		levelLabel.setText(String.valueOf(totalLevel));
		levelLabel.setToolTipText(QuantityFormatter.formatNumber(totalLevel));

		rankLabel.setText(Format.formatNumber(overall.getRank()));
		rankLabel.setToolTipText(QuantityFormatter.formatNumber(overall.getRank()));

		ehpLabel.setText(Format.formatNumber(overall.getEhp()));
		ehpLabel.setToolTipText(QuantityFormatter.formatNumber(overall.getEhp()));
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

	private void openPlayerProfile(String username)
	{
		String url = new HttpUrl.Builder()
			.scheme("https")
			.host("wiseoldman.net")
			.addPathSegment("players")
			.addPathSegment(username)
			.build()
			.toString();

		SwingUtilities.invokeLater(() -> LinkBrowser.browse(url));
	}
}
