package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class Skill implements WomResult
{
	int rank;
	long experience;
	double ehp;
}
