package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class GroupMemberAddition
{
    String verificationCode;
    Member[] members;
}