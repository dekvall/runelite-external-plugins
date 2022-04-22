package dev.dkvl.womutils.beans;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PlayerBuild
{
    @SerializedName("1def")
    ONE_DEF_PURE("1 Def Pure"),

    @SerializedName("lvl3")
    LEVEL_3("Level 3"),

    @SerializedName("f2p")
    F2P("F2P"),

    @SerializedName("10hp")
    HP_PURE("10 HP Pure"),

    @SerializedName("main")
    MAIN("Main"),

    @SerializedName("zerker")
    ZERKER("Zerker");

    private final String build;

    @Override
    public String toString()
    {
        return build;
    }
}
