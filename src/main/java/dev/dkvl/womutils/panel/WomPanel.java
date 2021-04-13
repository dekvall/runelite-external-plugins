package dev.dkvl.womutils.panel;

import com.google.common.base.Strings;
import dev.dkvl.womutils.Format;
import dev.dkvl.womutils.NameAutocompleter;
import dev.dkvl.womutils.WomClient;
import dev.dkvl.womutils.WomUtilsConfig;
import dev.dkvl.womutils.beans.PlayerInfo;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.LinkBrowser;
import net.runelite.http.api.hiscore.HiscoreEndpoint;
import okhttp3.HttpUrl;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

@Slf4j
public class WomPanel extends PluginPanel
{

    /* The maximum allowed username length in RuneScape accounts */
    private static final int MAX_USERNAME_LENGTH = 12;

    private final SkillingPanel skillingPanel;
    private final BossingPanel bossingPanel;
    private final ActivitiesPanel activitiesPanel;

    private final NameAutocompleter nameAutoCompleter;
    private final WomClient womClient;
    private final WomUtilsConfig config;

    private final IconTextField searchBar;

    private final java.util.List<InfoLabel> miscInfoLabels = new ArrayList<>();
    private final java.util.List<JButton> buttons = new ArrayList<>();

    /* The currently selected endpoint */
    private HiscoreEndpoint selectedEndPoint;

    /* Used to prevent users from switching endpoint tabs while the results are loading */
    private boolean loading = false;

    @Inject
    public WomPanel(Client client, NameAutocompleter nameAutocompleter, WomClient womClient, WomUtilsConfig config, SkillingPanel skillingPanel, BossingPanel bossingPanel, ActivitiesPanel activitiesPanel)
    {
        this.nameAutoCompleter = nameAutocompleter;
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

        for (InfoLabel infoLabel : miscInfoLabels)
        {
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

        this.skillingPanel = skillingPanel;
        this.bossingPanel = bossingPanel;
        this.activitiesPanel = activitiesPanel;

        // Holds currently visible tab
        JPanel display = new JPanel();
        MaterialTabGroup tabGroup = new MaterialTabGroup(display);
        MaterialTab skillingTab = new MaterialTab("Skills", tabGroup, skillingPanel);
        MaterialTab bossingTab = new MaterialTab("Bosses", tabGroup, bossingPanel);
        MaterialTab activitiesTab = new MaterialTab("Activities", tabGroup, activitiesPanel);

        tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));
        tabGroup.addTab(skillingTab);
        tabGroup.addTab(bossingTab);
        tabGroup.addTab(activitiesTab);
        tabGroup.select(skillingTab);

        add(tabGroup, c);
        c.gridy++;
        add(display, c);
        c.gridy++;

        addInputKeyListener(nameAutocompleter);
    }

    public void shutdown()
    {
        removeInputKeyListener(nameAutoCompleter);
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

        // Reset overview info
        for (InfoLabel infoLabel : miscInfoLabels)
        {
            infoLabel.getLabel().setText(infoLabel.getRawString() + ": --");
        }

        skillingPanel.reset();
        bossingPanel.reset();
        activitiesPanel.reset();

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
                applyOverviewResult(result);
                skillingPanel.update(result);
                bossingPanel.update(result);
                activitiesPanel.update(result);
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
                    String country = result.getCountry();
                    String countryTxt = country == null ? "--" : country;
                    label.setText("Country: " + countryTxt);
                    break;
                case "build":
                    label.setText("Build: " + result.getBuild());
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
