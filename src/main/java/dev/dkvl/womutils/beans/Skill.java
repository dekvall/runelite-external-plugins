package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class Skill
{
	String metric;
	long experience;
	int rank;
	int level;
	double ehp;
}
