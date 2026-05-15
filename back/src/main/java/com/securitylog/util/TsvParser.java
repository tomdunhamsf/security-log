package com.securitylog.util;

import com.securitylog.entity.LogEntry;

/**
 * Parses tab-delimited Zscaler log lines with columns in fixed order:
 * time  cip  sip  login  ua  method  url  respcode  reqhdrsize  reqsize  resphrdsize  respsize  referrer
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

    private TsvParser() {}

    /**
     * Returns null for blank lines and header rows (first column == "time").
     */
    public static LogEntry parseLine(String line, String logName) {
        if (line == null || line.isBlank()) return null;

        String[] cols = line.split("\t", -1);
        if (cols.length < COL_URL + 1) return null;

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
