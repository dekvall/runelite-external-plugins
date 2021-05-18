package dev.dkvl.womutils.panel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MiscInfo
{
    BUILD("Build", "../build.png", "--"),
    COUNTRY("Country", "../flags_square/default.png", "--"),
    TTM("TTM", "../ttm.png", "--"),
    EHP("EHP", "../ehp.png", "--"),
    EHB("EHB", "../bosses/ehb.png", "--"),
    EXP("Exp", "../overall.png", "--"),
    LAST_UPDATED("Last updated", "", "Last updated --");

    private final String rawString;
    private final String iconPath;
    private final String clearedString;
}
