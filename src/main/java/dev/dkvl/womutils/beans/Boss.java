package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class Boss implements WomResult
{
	int rank;
	int kills;
	double ehb;
}
