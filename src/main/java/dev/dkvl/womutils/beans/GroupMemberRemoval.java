package dev.dkvl.womutils.beans;

import com.google.gson.JsonObject;
import lombok.Value;

import java.util.ArrayList;

@Value
public class GroupMemberRemoval
{
    String verificationCode;
    ArrayList<String> members;
}