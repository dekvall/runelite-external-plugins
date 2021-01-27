package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class GroupMemberRemoval implements WomResult
{
    String verificationCode;
    String[] members;
}