package dev.dkvl.womutils.beans;

import lombok.Value;
import net.runelite.client.hiscore.HiscoreSkill;

@Value
public class SnapshotSkills
{
    Skill overall;
    Skill attack;
    Skill defence;
    Skill strength;
    Skill hitpoints;
    Skill ranged;
    Skill prayer;
    Skill magic;
    Skill cooking;
    Skill woodcutting;
    Skill fletching;
    Skill fishing;
    Skill firemaking;
    Skill crafting;
    Skill smithing;
    Skill mining;
    Skill herblore;
    Skill agility;
    Skill thieving;
    Skill slayer;
    Skill farming;
    Skill runecrafting;
    Skill hunter;
    Skill construction;

    public Skill getSkill(HiscoreSkill skill)
    {
        switch (skill)
        {
            case ATTACK:
                return getAttack();
            case DEFENCE:
                return getDefence();
            case STRENGTH:
                return getStrength();
            case HITPOINTS:
                return getHitpoints();
            case RANGED:
                return getRanged();
            case PRAYER:
                return getPrayer();
            case MAGIC:
                return getMagic();
            case COOKING:
                return getCooking();
            case WOODCUTTING:
                return getWoodcutting();
            case FLETCHING:
                return getFletching();
            case FISHING:
                return getFishing();
            case FIREMAKING:
                return getFiremaking();
            case CRAFTING:
                return getCrafting();
            case SMITHING:
                return getSmithing();
            case MINING:
                return getMining();
            case HERBLORE:
                return getHerblore();
            case AGILITY:
                return getAgility();
            case THIEVING:
                return getThieving();
            case SLAYER:
                return getSlayer();
            case FARMING:
                return getFarming();
            case RUNECRAFT:
                return getRunecrafting();
            case HUNTER:
                return getHunter();
            case CONSTRUCTION:
                return getConstruction();
            case OVERALL:
                return getOverall();
            default:
                throw new IllegalArgumentException("Invalid hiscore skill");
        }
    }
}