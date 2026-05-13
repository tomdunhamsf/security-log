package com.securitylog.util;

import com.securitylog.entity.LogEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZscalerParser {

    // Leading syslog-style timestamp: Mon DD HH:MM:SS YYYY
    private static final Pattern TIMESTAMP = Pattern.compile(
            "^(\\w+\\s+\\d+\\s+\\d{2}:\\d{2}:\\d{2}\\s+\\d{4})\\s");

    // key=value or key="quoted value"
    private static final Pattern KV = Pattern.compile(
            "(\\w+)=(\"(?:[^\"\\\\]|\\\\.)*\"|[^\\s]+)");

    private ZscalerParser() {}

    /**
     * Parse a single Zscaler NSS web log line into a LogEntry.
     * Returns null if the line yields no key=value pairs.
     */
    public static LogEntry parseLine(String line, String logName) {
        if (line == null || line.isBlank()) {
            return null;
        }

        LogEntry entry = new LogEntry();
        entry.setLogName(logName);

        Matcher ts = TIMESTAMP.matcher(line);
        if (ts.find()) {
            entry.setTime(ts.group(1).replaceAll("\\s+", " "));
        }

        Map<String, String> kv = extractKV(line);
        if (kv.isEmpty()) {
            return null;
        }

        entry.setCip(kv.get("clientip"));
        entry.setSip(kv.get("serverip"));
        entry.setLogin(kv.get("username"));
        entry.setUa(truncate(kv.get("useragent"), 1024));
        // Accept both 'reqmethod' (NSS default) and 'httpmethod'
        entry.setMethod(kv.getOrDefault("reqmethod", kv.get("httpmethod")));
        entry.setUrl(truncate(kv.get("url"), 2048));
        entry.setRespcode(kv.get("status"));
        entry.setReqhdrsize(kv.get("reqhdrsize"));
        entry.setReqsize(kv.get("reqsize"));
        // NSS uses 'resphdrsize'; DB column is 'resphrdsize' (per spec)
        entry.setResphrdsize(kv.get("resphdrsize"));
        entry.setRespsize(kv.get("respsize"));
        // Accept both 'refererurl' and 'referrer'
        String referrer = kv.getOrDefault("refererurl", kv.get("referrer"));
        entry.setReferrer(truncate(referrer, 2048));

        return entry;
    }

    private static Map<String, String> extractKV(String line) {
        Map<String, String> map = new HashMap<>();
        Matcher m = KV.matcher(line);
        while (m.find()) {
            String key = m.group(1).toLowerCase();
            String raw = m.group(2);
            String value = raw.startsWith("\"") ? raw.substring(1, raw.length() - 1) : raw;
            map.put(key, value);
        }
        return map;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
