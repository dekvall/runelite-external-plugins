package dev.dkvl.womutils.beans;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PlayerType
{
    @SerializedName("unknown")
    UNKNOWN("Unknown"),

    @SerializedName("regular")
    REGULAR("Regular"),

    @SerializedName("ironman")
    IRONMAN("Ironman"),

    @SerializedName("hardcore")
    HARDCORE("Hardcore"),

    @SerializedName("ultimate")
    ULTIMATE("Ultimate");

    private final String type;

    @Override
    public String toString()
    {
        return type;
    }
}
