package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class Minigame implements WomResult
{
	int rank;
	int score;
}
