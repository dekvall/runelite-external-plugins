package dev.dkvl.womutils.events;

import dev.dkvl.womutils.beans.MemberInfo;
import lombok.Value;

@Value
public class WomGroupMemberRemoved
{
	String username;
}
