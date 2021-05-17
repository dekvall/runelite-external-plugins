package dev.dkvl.womutils.panel;

import com.google.common.base.Strings;
import dev.dkvl.womutils.*;
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
import net.runelite.client.util.ImageUtil;
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

    private final NameAutocompleter nameAutocompleter;
    private final WomClient womClient;
    private final WomUtilsConfig config;

    private final IconTextField searchBar;

    private final java.util.List<MiscInfo> miscInfoLabels = new ArrayList<>();
    private final java.util.List<JButton> buttons = new ArrayList<>();

    /* The currently selected endpoint */
    private HiscoreEndpoint selectedEndPoint;

    /* Used to prevent users from switching endpoint tabs while the results are loading */
    private boolean loading = false;

    @Inject
    public WomPanel(Client client, NameAutocompleter nameAutocompleter, WomClient womClient, WomUtilsConfig config,
                    SkillingPanel skillingPanel, BossingPanel bossingPanel, ActivitiesPanel activitiesPanel)
    {
        this.nameAutocompleter = nameAutocompleter;
        this.womClient = womClient;
        this.config = config;
        this.skillingPanel = skillingPanel;
        this.bossingPanel = bossingPanel;
        this.activitiesPanel = activitiesPanel;


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

        add(createButtonsPanel(), c);

        c.gridy++;

        MiscInfo lastUpdated = MiscInfo.LAST_UPDATED;
        miscInfoLabels.add(MiscInfo.BUILD);
        miscInfoLabels.add(MiscInfo.COUNTRY);
        miscInfoLabels.add(MiscInfo.TTM);
        miscInfoLabels.add(MiscInfo.EHP);
        miscInfoLabels.add(MiscInfo.EHB);
        miscInfoLabels.add(MiscInfo.EXP);
        miscInfoLabels.add(lastUpdated);

        JLabel overviewTitle = new JLabel("Overview");
        overviewTitle.setFont(FontManager.getRunescapeBoldFont());
        add(overviewTitle, c);
        c.gridy++;

        add(createOverViewPanel(), c);
        c.gridy++;

        add(lastUpdated.getLabel(), c);
        c.gridy++;

        // Holds currently visible tab
        JPanel display = new JPanel();
        MaterialTabGroup tabGroup = new MaterialTabGroup(display);
        MaterialTab skillingTab = new MaterialTab("Skills", tabGroup, skillingPanel);
        MaterialTab bossingTab = new MaterialTab("Bosses", tabGroup, bossingPanel);
        MaterialTab activitiesTab = new MaterialTab("Activities", tabGroup, activitiesPanel);

        tabGroup.setBorder(new EmptyBorder(10, 0, 0, 0));
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
        resetOverview();
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

        resetOverview();
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
                applyResult(result);
            }));
    }

    private void applyOverviewResult(PlayerInfo result)
    {
        for (MiscInfo infoLabel : miscInfoLabels)
        {
            JLabel label = infoLabel.getLabel();
            switch (infoLabel)
            {
                case COUNTRY:
                    String country = result.getCountry();
                    String countryTxt = country == null ? "--" : country;
                    String languageCode = country == null ? "default" : country.toLowerCase();
                    label.setIcon(CountryIcon.loadSquareImage(languageCode));
                    label.setText(countryTxt);
                    break;
                case BUILD:
                    label.setText("" + result.getBuild());
                    break;
                case TTM:
                    label.setText(Format.formatNumber(result.getTtm()) + 'h');
                    break;
                case EHP:
                    label.setText(Format.formatNumber(result.getEhp()));
                    break;
                case EHB:
                    label.setText(Format.formatNumber(result.getEhb()));
                    break;
                case EXP:
                    label.setText(Format.formatNumber(result.getExp()));
                    break;
                case LAST_UPDATED:
                    label.setText("Last updated " + Format.formatDate(result.getUpdatedAt(), config.relativeTime()));
                    break;
            }
        }
    }

    private void resetOverview()
    {
        for (MiscInfo infoLabel : miscInfoLabels)
        {
            JLabel label = infoLabel.getLabel();
            label.setText("--");

            switch (infoLabel)
            {
                case COUNTRY:
                    label.setIcon(CountryIcon.loadSquareImage("default"));
                    break;
                case LAST_UPDATED:
                    label.setText("Last updated --");
                    break;
            }
        }
    }

    private void applyResult(PlayerInfo result)
    {
        assert SwingUtilities.isEventDispatchThread();

        nameAutocompleter.addToSearchHistory(result.getUsername());

        applyOverviewResult(result);
        skillingPanel.update(result);
        bossingPanel.update(result);
        activitiesPanel.update(result);
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

    private JPanel createButtonsPanel()
    {
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridBagLayout());
        buttonsPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.ipady = 10;

        JButton updateButton = new JButton();
        updateButton.setFont(FontManager.getRunescapeSmallFont());
        updateButton.setEnabled(false);
        updateButton.addActionListener(e ->
            womClient.updatePlayer(sanitize(searchBar.getText())));
        updateButton.setText("Update");

        JButton profileButton = new JButton();
        profileButton.setFont(FontManager.getRunescapeSmallFont());
        profileButton.setEnabled(false);
        profileButton.addActionListener(e ->
            openPlayerProfile(sanitize(searchBar.getText())));
        profileButton.setText("Open Profile");

        buttons.add(updateButton);
        buttons.add(profileButton);

        buttonsPanel.add(updateButton, gbc);
        gbc.gridx++;
        gbc.insets.left = 7;
        buttonsPanel.add(profileButton, gbc);

        return buttonsPanel;
    }

    private JPanel createOverViewPanel()
    {
        JPanel miscInfoPanel = new JPanel();
        miscInfoPanel.setLayout(new GridLayout(3, 2, 5, 5));

        for (MiscInfo infoLabel : miscInfoLabels)
        {
            JLabel label = infoLabel.getLabel();
            ImageIcon icon;

            switch (infoLabel)
            {
                case COUNTRY:
                    icon = CountryIcon.loadSquareImage("default");
                    break;
                case TTM:
                    icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "../ttm.png"));
                    break;
                case EXP:
                    icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "../overall.png"));
                    break;
                case EHB:
                    icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "../bosses/ehb.png"));
                    break;
                case EHP:
                    icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "../ehp.png"));
                    break;
                case BUILD:
                    icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "../build.png"));
                    break;
                default:
                    label.setHorizontalAlignment(JLabel.CENTER);
                    continue;
            }

            label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            label.setOpaque(true);
            label.setFont(FontManager.getRunescapeSmallFont());
            label.setBorder(new EmptyBorder(5, 10, 5, 5));
            label.setToolTipText(infoLabel.getRawString());
            label.setIcon(icon);

            miscInfoPanel.add(label);
        }

        return miscInfoPanel;
    }
}
