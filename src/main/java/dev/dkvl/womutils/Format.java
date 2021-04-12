/*
 * Copyright (c) 2021, Rorro <https://github.com/rorro>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.dkvl.womutils;

import net.runelite.http.api.hiscore.HiscoreSkill;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class Format {
    public static String formatNumber(long num)
    {
        if ((num < 10000 && num > -10000))
        {
            return String.valueOf(num);
        }

        DecimalFormat df = new DecimalFormat();
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.FLOOR);
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        df.setDecimalFormatSymbols(formatSymbols);
        df.setMaximumFractionDigits(2);

        // < 10 million
        if (num < 10_000_000 && num > -10_000_000) {
            df.setMaximumFractionDigits(0);
            return df.format(num / 1000.0) + "k";
        }

        // < 1 billion
        if (num < 1_000_000_000 && num > -1_000_000_000) {
            return df.format( num / 1_000_000.0) + "m";
        }

        return df.format(num / 1_000_000_000.0) + "b";
    }

    public static String formatNumber(double num)
    {
        if ((num < 10000 && num > -10000))
        {
            return String.format("%.0f", num);
        }

        DecimalFormat df = new DecimalFormat();
        df.setRoundingMode(RoundingMode.FLOOR);
        df.setMaximumFractionDigits(2);

        return df.format(num / 1000.0) + "k";
    }

    public static String formatDate(String date, boolean relative)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        ZoneId localZone = ZoneId.systemDefault();
        ZonedDateTime updatedAt = Instant.parse(date).atZone(localZone);

        if (relative)
        {
            String lastUpdated = "";
            ZonedDateTime now = Instant.now().atZone(localZone);
            long difference = Duration.between(updatedAt, now).toHours();

            if (difference == 0)
            {
                return "< 60 minutes ago";
            }

            long days = difference / 24;
            long hours = difference % 24;

            String dayUnit = days > 1 ? " days, " : "day, ";
            String hourUnit = hours > 1 ? " hours ago" : " hour ago";

            lastUpdated += days > 0 ? days + dayUnit : "";
            lastUpdated += hours > 0 ? hours + hourUnit : "";

            return lastUpdated;
        }
        else
        {
            return formatter.format(updatedAt);
        }
    }

    public static int getMinimumKc(HiscoreSkill boss) {
        switch (boss.name().toLowerCase()) {
            case "mimic":
            case "tzkal_zuk":
                return 2;
            case "bryophyta":
            case "chambers_of_xeric_challenge_mode":
            case "hespori":
            case "obor":
            case "skotizo":
            case "the_corrupted_gauntlet":
            case "tztok_jad":
                return 10;
            default:
                return 50;
        }
    }
}
