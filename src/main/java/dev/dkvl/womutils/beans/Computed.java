package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class Computed
{
    String metric;
    double value;
    int rank;
}
