package dev.dkvl.womutils.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GroupInfoWithMemberships extends GroupInfo
{
    GroupMembership[] memberships;
}
