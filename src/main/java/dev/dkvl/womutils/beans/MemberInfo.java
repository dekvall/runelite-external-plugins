package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class MemberInfo
{
    long exp;
    int id;
    String username;
    String displayName;
    String type;
    String build;
    String country;
    boolean flagged;
    double ehp;
    double ehb;
    double ttm;
    double tt200m;
    String lastImportedAt;
    String lastChangedAt;
    String registeredAt;
    String updatedAt;
    String role;
    String joinedAt;
}