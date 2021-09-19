package dev.dkvl.womutils.beans;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompetitionType
{
	@SerializedName("classic")
	CLASSIC("Classic"),
	@SerializedName("team")
	TEAM("Team")
	;

	private String type;

	public String toString()
	{
		return type;
	}
}
