package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class GroupMemberAddition implements WomResult
{
    String verificationCode;
    Member[] members;
}