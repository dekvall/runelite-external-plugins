package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class NameChangeEntry implements WomResult
{
	String oldName;
	String newName;
}
