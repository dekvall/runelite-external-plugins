package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class Competition
{
	int id;
	String title;
	String metric;
	int score;
	String startsAt;
	String endsAt;
	String type;
	int groupId;
	String createdAt;
	String updatedAt;
	String duration;
	int particiantCount;
}
