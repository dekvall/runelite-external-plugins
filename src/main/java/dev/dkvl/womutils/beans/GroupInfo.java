package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class GroupInfo
{
    int id;
    String name;
    String clanChat;
    String description;
    int homeworld;
    int score;
    boolean verified;
    String createdAt;
    String updatedAt;
    MemberInfo[] members;
}
