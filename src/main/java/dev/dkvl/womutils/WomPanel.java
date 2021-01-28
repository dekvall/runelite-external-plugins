/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * Copyright (c) 2019, Bram91 <https://github.com/bram91>
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import dev.dkvl.womutils.beans.Boss;
import dev.dkvl.womutils.beans.PlayerInfo;
import dev.dkvl.womutils.beans.Skill;
import dev.dkvl.womutils.beans.Snapshot;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.hiscore.HiscoreEndpoint;
import net.runelite.http.api.hiscore.HiscoreSkill;
import static net.runelite.http.api.hiscore.HiscoreSkill.*;
import net.runelite.http.api.hiscore.HiscoreSkillType;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class WomPanel extends PluginPanel
{
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
		SCORPIA, SKOTIZO, THE_GAUNTLET,
		THE_CORRUPTED_GAUNTLET, THEATRE_OF_BLOOD, THERMONUCLEAR_SMOKE_DEVIL,
		TZKAL_ZUK, TZTOK_JAD, VENENATIS,
		VETION, VORKATH, WINTERTODT,
		ZALCANO, ZULRAH
	);

	private final Client client;
	private final NameAutocompleter nameAutocompleter;
	private final WomClient womClient;

	private final IconTextField searchBar;
//	private final JButton updateBtn;

	// Not an enummap because we need null keys for combat
	private final Map<HiscoreSkill, JLabel> skillLabels = new HashMap<>();

	/* The currently selected endpoint */
	private HiscoreEndpoint selectedEndPoint;

	/* Used to prevent users from switching endpoint tabs while the results are loading */
	private boolean loading = false;

	@Inject
	public WomPanel(Client client, NameAutocompleter nameAutocompleter, WomClient womClient)
	{
		this.client = client;
		this.nameAutocompleter = nameAutocompleter;
		this.womClient = womClient;

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
//		c.gridy++;
//
//		updateBtn = new JButton("Update");
//		updateBtn.addActionListener(ev -> updateAndLookup());
//		add(updateBtn);

		c.gridy++;

		// TODO: Add a tab/hover that displays the current rate for the selected player
		// TODO: Could just do one request on startup, and then it could show in the hover maybe instead

		// Panel that holds skill icons
		JPanel statsPanel = new JPanel();
		statsPanel.setLayout(new GridLayout(8, 3));
		statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statsPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

		// For each skill on the ingame skill panel, create a Label and add it to the UI
		for (HiscoreSkill skill : SKILLS)
		{
			JPanel panel = makeHiscorePanel(skill);
			statsPanel.add(panel);
		}

		add(statsPanel, c);
		c.gridy++;

		JPanel totalPanel = new JPanel();
		totalPanel.setLayout(new GridLayout(1, 2));
		totalPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		totalPanel.add(makeHiscorePanel(null)); //combat has no hiscore skill, referred to as null
		totalPanel.add(makeHiscorePanel(OVERALL));

		add(totalPanel, c);
		c.gridy++;

		JPanel bossPanel = new JPanel();
		bossPanel.setLayout(new GridLayout(0, 3));
		bossPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// For each boss on the hi-scores, create a Label and add it to the UI
		for (HiscoreSkill skill : BOSSES)
		{
			JPanel panel = makeHiscorePanel(skill);
			bossPanel.add(panel);
		}

		add(bossPanel, c);
		c.gridy++;

		addInputKeyListener(nameAutocompleter);
	}

	void shutdown()
	{
		removeInputKeyListener(nameAutocompleter);
	}

	@Override
	public void onActivate()
	{
		super.onActivate();
		searchBar.requestFocusInWindow();
	}

	/* Builds a JPanel displaying an icon and level/number associated with it */
	private JPanel makeHiscorePanel(HiscoreSkill skill)
	{
		HiscoreSkillType skillType = skill == null ? HiscoreSkillType.SKILL : skill.getType();

		JLabel label = new JLabel();
		label.setToolTipText(skill == null ? "Combat" : skill.getName());
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setText(pad("--", skillType));
		label.setForeground(Color.WHITE);

		String directory;
		if (skill == null || skill == OVERALL)
		{
			directory = "/skill_icons/";
		}
		else if (skill.getType() == HiscoreSkillType.BOSS)
		{
			directory = "bosses/";
		}
		else
		{
			directory = "/skill_icons_small/";
		}

		String skillName = (skill == null ? "combat" : skill.name().toLowerCase());
		String skillIcon = directory + skillName + ".png";
		log.debug("Loading skill icon from {}", skillIcon);

		label.setIcon(new ImageIcon(ImageUtil.loadImageResource(getClass(), skillIcon)));

		boolean totalLabel = skill == OVERALL || skill == null; //overall or combat
		label.setIconTextGap(totalLabel ? 10 : 4);

		JPanel skillPanel = new JPanel();
		skillPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		skillPanel.setBorder(new EmptyBorder(2, 0, 2, 0));
		skillLabels.put(skill, label);
		skillPanel.add(label);

		return skillPanel;
	}

	private void updateAndLookup()
	{
		String player = sanitize(searchBar.getText());
	}

	public void lookup(String username)
	{
		searchBar.setText(username);
		lookup();
	}

	private void lookup()
	{
		final String lookup = sanitize(searchBar.getText());

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

		for (Map.Entry<HiscoreSkill, JLabel> entry : skillLabels.entrySet())
		{
			HiscoreSkill skill = entry.getKey();
			JLabel label = entry.getValue();
			HiscoreSkillType skillType = skill == null ? HiscoreSkillType.SKILL : skill.getType();

			label.setText(pad("--", skillType));
			label.setToolTipText(skill == null ? "Combat" : skill.getName());
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
					return;
				}

				//successful player search
				searchBar.setIcon(IconTextField.Icon.SEARCH);
				searchBar.setEditable(true);
				loading = false;
				log.info("{}", searchBar.getFont());

				applyResult(result);
			}));
	}

	private void applyResult(PlayerInfo result)
	{
		assert SwingUtilities.isEventDispatchThread();

		nameAutocompleter.addToSearchHistory(result.getUsername());

		for (Map.Entry<HiscoreSkill, JLabel> entry : skillLabels.entrySet())
		{
			HiscoreSkill skill = entry.getKey();
			JLabel label = entry.getValue();

			if (skill == null)
			{
				label.setText(Integer.toString(result.getCombatLevel()));
			}
			else if (skill.getType() == HiscoreSkillType.SKILL || skill.getType() == HiscoreSkillType.OVERALL)
			{
				dev.dkvl.womutils.beans.Skill s = result.getLatestSnapshot().getSkill(skill);
				label.setText(pad(formatHours(s.getEhp()), skill.getType()));
			}
			else if (skill.getType() == HiscoreSkillType.BOSS)
			{
				Boss b = result.getLatestSnapshot().getBoss(skill);
				label.setText(pad(formatHours(b.getEhb()), skill.getType()));
			}

			if (skill != null)
			{
				label.setToolTipText(detailsHtml(result.getLatestSnapshot(), skill));
			}
		}
	}

	void addInputKeyListener(KeyListener l)
	{
		this.searchBar.addKeyListener(l);
	}

	void removeInputKeyListener(KeyListener l)
	{
		this.searchBar.removeKeyListener(l);
	}

	/*
		Builds a html string to display on tooltip (when hovering a skill).
	 */
	private String detailsHtml(Snapshot snapshot, HiscoreSkill skill)
	{
		String openingTags = "<html><body style = 'padding: 5px;color:#989898'>";
		String closingTags = "</html><body>";

		String content = "";

		if (skill == OVERALL)
		{
				dev.dkvl.womutils.beans.Skill requestedSkill = snapshot.getSkill(skill);
				String rank = (requestedSkill.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(requestedSkill.getRank());
				String exp = (requestedSkill.getExperience() == -1L) ? "Unranked" : QuantityFormatter.formatNumber(requestedSkill.getExperience());
				content += "<p><span style = 'color:white'>" + skill.getName() + "</span></p>";
				content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
				content += "<p><span style = 'color:white'>Experience:</span> " + exp + "</p>";
		}
		else
		{
			if (skill.getType() == HiscoreSkillType.BOSS)
			{
				String rank = "Unranked";
				String lvl = null;
				Boss requestedSkill = snapshot.getBoss(skill);
				if (requestedSkill != null)
				{
					if (requestedSkill.getRank() > -1)
					{
						rank = QuantityFormatter.formatNumber(requestedSkill.getRank());
					}
					if (requestedSkill.getKills() > -1)
					{
						lvl = QuantityFormatter.formatNumber(requestedSkill.getKills());
					}
				}

				content += "<p><span style = 'color:white'>Boss:</span> " + skill.getName() + "</p>";
				content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
				if (lvl != null)
				{
					content += "<p><span style = 'color:white'>KC:</span> " + lvl + "</p>";
				}
			}
			else
			{
				Skill requestedSkill = snapshot.getSkill(skill);
				final long experience = requestedSkill.getExperience();

				String rank = (requestedSkill.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(requestedSkill.getRank());
				String exp = (experience == -1L) ? "Unranked" : QuantityFormatter.formatNumber(experience);
				String remainingXp;
				if (experience == -1L)
				{
					remainingXp = "Unranked";
				}
				else
				{
					int currentLevel = Experience.getLevelForXp((int) experience);
					remainingXp = (currentLevel + 1 <= Experience.MAX_VIRT_LEVEL) ? QuantityFormatter.formatNumber(Experience.getXpForLevel(currentLevel + 1) - experience) : "0";
				}

				content += "<p><span style = 'color:white'>Skill:</span> " + skill.getName() + "</p>";
				content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
				content += "<p><span style = 'color:white'>Experience:</span> " + exp + "</p>";
				content += "<p><span style = 'color:white'>Remaining XP:</span> " + remainingXp + "</p>";
			}
		}
		return openingTags + content + closingTags;
	}

	private static String sanitize(String lookup)
	{
		return lookup.replace('\u00A0', ' ');
	}

	private HiscoreEndpoint selectWorldEndpoint()
	{
		if (client != null)
		{
			EnumSet<WorldType> wTypes = client.getWorldType();

			if (wTypes.contains(WorldType.DEADMAN_TOURNAMENT))
			{
				return HiscoreEndpoint.TOURNAMENT;
			}
			else if (wTypes.contains(WorldType.DEADMAN))
			{
				return HiscoreEndpoint.DEADMAN;
			}
			else if (wTypes.contains(WorldType.LEAGUE))
			{
				return HiscoreEndpoint.LEAGUE;
			}
		}
		return HiscoreEndpoint.NORMAL;
	}

	@VisibleForTesting
	static String formatHours(double hours)
	{
		int h = (int) hours;
		if (h < 1)
		{
			return "--";
		}
		else if (h < 10000)
		{
			return h + "h";
		}
		else
		{
			return (h / 1000) + "kh";
		}
	}

	private static String pad(String str, HiscoreSkillType type)
	{
		// Left pad label text to keep labels aligned
		int pad = type == HiscoreSkillType.BOSS ? 4 : 2;
		return StringUtils.leftPad(str, pad);
	}
}
