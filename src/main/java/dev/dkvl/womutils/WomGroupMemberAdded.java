package dev.dkvl.womutils;

import dev.dkvl.womutils.beans.MemberInfo;
import lombok.Value;

@Value
public class WomGroupMemberAdded
{
	String username;
	MemberInfo member;
}
