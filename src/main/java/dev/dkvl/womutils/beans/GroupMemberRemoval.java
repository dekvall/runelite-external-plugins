package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class GroupMemberRemoval
{
    String verificationCode;
    String[] members;
}