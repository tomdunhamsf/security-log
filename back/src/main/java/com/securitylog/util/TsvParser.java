package com.securitylog.util;

import com.securitylog.entity.LogEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses keyless log lines with columns in fixed order:
 * time  cip  sip  login  ua  method  url  respcode  reqhdrsize  reqsize  resphrdsize  respsize  referrer
 *
 * Handles two physical formats:
 *   1. True tab-separated (`\t` between every column)
 *   2. Whitespace-padded — the date has an internal space (e.g. "05-13-2026 09:15:01:123")
 *      and the user-agent contains spaces, so the regex anchors on the date format
 *      at the start and the HTTP method as the boundary after the UA.
 */
public class TsvParser {

    private static final int COL_TIME        = 0;
    private static final int COL_CIP         = 1;
    private static final int COL_SIP         = 2;
    private static final int COL_LOGIN       = 3;
    private static final int COL_UA          = 4;
    private static final int COL_METHOD      = 5;
    private static final int COL_URL         = 6;
    private static final int COL_RESPCODE    = 7;
    private static final int COL_REQHDRSIZE  = 8;
    private static final int COL_REQSIZE     = 9;
    private static final int COL_RESPHRDSIZE = 10;
    private static final int COL_RESPSIZE    = 11;
    private static final int COL_REFERRER    = 12;

    private static final Pattern WHITESPACE_LINE = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}:\\d{3})\\s+" +  // time
            "(\\S+)\\s+" +                                                    // cip
            "(\\S+)\\s+" +                                                    // sip
            "(\\S+)\\s+" +                                                    // login
            "(.+?)\\s+" +                                                     // ua (lazy)
            "(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH|CONNECT|TRACE)\\s+" +    // method
            "(\\S+)\\s+" +                                                    // url
            "(\\S+)\\s+" +                                                    // respcode
            "(\\S+)\\s+" +                                                    // reqhdrsize
            "(\\S+)\\s+" +                                                    // reqsize
            "(\\S+)\\s+" +                                                    // resphrdsize
            "(\\S+)\\s+" +                                                    // respsize
            "(\\S+)\\s*$"                                                     // referrer
    );

    private TsvParser() {}

    public static LogEntry parseLine(String line, String logName) {
        if (line == null || line.isBlank()) return null;

        String[] cols;
        if (line.contains("\t")) {
            cols = line.split("\t", -1);
            if (cols.length < COL_URL + 1) return null;
        } else {
            cols = splitWhitespacePadded(line);
            if (cols == null) return null;
        }

        // Skip a header row if present
        if (cols[COL_TIME].trim().equalsIgnoreCase("time")) return null;

        LogEntry entry = new LogEntry();
        entry.setLogName(logName);
        entry.setTime(col(cols, COL_TIME));
        entry.setCip(col(cols, COL_CIP));
        entry.setSip(col(cols, COL_SIP));
        entry.setLogin(col(cols, COL_LOGIN));
        entry.setUa(truncate(col(cols, COL_UA), 1024));
        entry.setMethod(col(cols, COL_METHOD));
        entry.setUrl(truncate(col(cols, COL_URL), 2048));
        entry.setRespcode(col(cols, COL_RESPCODE));
        entry.setReqhdrsize(col(cols, COL_REQHDRSIZE));
        entry.setReqsize(col(cols, COL_REQSIZE));
        entry.setResphrdsize(col(cols, COL_RESPHRDSIZE));
        entry.setRespsize(col(cols, COL_RESPSIZE));
        entry.setReferrer(truncate(col(cols, COL_REFERRER), 2048));

        return entry;
    }

    private static String[] splitWhitespacePadded(String line) {
        Matcher m = WHITESPACE_LINE.matcher(line.trim());
        if (!m.matches()) return null;
        String[] out = new String[13];
        for (int i = 0; i < 13; i++) {
            out[i] = m.group(i + 1);
        }
        return out;
    }

    private static String col(String[] cols, int idx) {
        if (idx >= cols.length) return null;
        String v = cols[idx].trim();
        return (v.isEmpty() || v.equals("-")) ? null : v;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
