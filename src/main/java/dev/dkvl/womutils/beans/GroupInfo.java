package dev.dkvl.womutils.beans;

import lombok.Data;

@Data
public class GroupInfo
{
    int id;
    String name;
    String clanChat;
    String description;
    int homeworld;
    boolean verified;
    int score;
    String createdAt;
    String updatedAt;
    int memberCount;
}
