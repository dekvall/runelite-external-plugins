package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class Member implements WomResult
{
    String username;
    String role = "member";
}