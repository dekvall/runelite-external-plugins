package dev.dkvl.tipoftheday.beans;

import lombok.Getter;

@Getter
public class TipEntry
{
    private String tip;
    // If url doesn't exist it uses the github wiki for the plugin as base
    private String url;
    private String pluginName;
}
