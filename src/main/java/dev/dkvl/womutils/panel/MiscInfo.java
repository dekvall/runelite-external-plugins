package dev.dkvl.womutils.panel;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum MiscInfo
{
    BUILD("Build", new JLabel("--")),
    COUNTRY("Country", new JLabel("--")),
    TTM("TTM", new JLabel("--")),
    EHP("EHP", new JLabel("--")),
    EHB("EHB", new JLabel("--")),
    EXP("Exp", new JLabel("--")),
    LAST_UPDATED("Last updated", new JLabel("Last updated --"));

    private final String rawString;
    private final JLabel label;
}
