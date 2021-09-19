package dev.dkvl.womutils.util;

import java.time.Duration;
import lombok.Value;

@Value
public class DelayedAction
{
	Duration delay;
	Runnable runnable;
}
