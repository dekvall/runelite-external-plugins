package dev.dkvl.womutils.beans;

import lombok.Value;
import net.runelite.client.hiscore.HiscoreSkill;

@Value
public class Snapshot
{
	int id;
	int playerId;
	String createdAt;
	String importedAt;
	SnapshotData data;
}
