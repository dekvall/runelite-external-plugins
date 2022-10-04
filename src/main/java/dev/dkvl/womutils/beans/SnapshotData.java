package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class SnapshotData
{
    SnapshotSkills skills;
    SnapshotBosses bosses;
    SnapshotActivities activities;
    SnapshotComputed computed;
}
