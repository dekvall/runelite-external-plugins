package dev.dkvl.womutils;

import dev.dkvl.womutils.beans.MemberInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WomGroupSynced
{
	MemberInfo[] members;
	boolean silent;

	public WomGroupSynced(MemberInfo[] members)
	{
		this(members, false);
	}
}
