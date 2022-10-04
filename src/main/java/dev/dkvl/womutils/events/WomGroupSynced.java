package dev.dkvl.womutils.events;

import dev.dkvl.womutils.beans.GroupInfoWithMemberships;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WomGroupSynced
{
	GroupInfoWithMemberships groupInfo;
	boolean silent;

	public WomGroupSynced(GroupInfoWithMemberships groupInfo)
	{
		this(groupInfo, false);
	}
}
