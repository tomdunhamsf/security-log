package com.securitylog.util;

import com.securitylog.entity.LogEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TsvParserTest {

    private static final String LOG_NAME = "test.log";

    @Test
    void parsesWhitespacePaddedLine() {
        String line = "05-14-2026 09:15:10:123 65.108.10.180 75.101.130.5    none    "
                + "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) "
                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/19.0 "
                + "Mobile/15E148 Safari/604.1   "
                + "GET www.america-yes.com/05-10-2026/i-love-war.htm   "
                + "200 600  600  354   2500   none";

        LogEntry entry = TsvParser.parseLine(line, LOG_NAME);

        assertNotNull(entry);
        assertEquals(LOG_NAME, entry.getLogName());
        assertEquals("05-14-2026 09:15:10:123", entry.getTime());
        assertEquals("65.108.10.180", entry.getCip());
        assertEquals("75.101.130.5", entry.getSip());
        assertEquals("none", entry.getLogin());
        assertEquals("Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) "
                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/19.0 "
                + "Mobile/15E148 Safari/604.1", entry.getUa());
        assertEquals("GET", entry.getMethod());
        assertEquals("www.america-yes.com/05-10-2026/i-love-war.htm", entry.getUrl());
        assertEquals("200", entry.getRespcode());
        assertEquals("600", entry.getReqhdrsize());
        assertEquals("600", entry.getReqsize());
        assertEquals("354", entry.getResphrdsize());
        assertEquals("2500", entry.getRespsize());
        assertEquals("none", entry.getReferrer());
    }

    @Test
    void parsesTabSeparatedLine() {
        String line = String.join("\t",
                "05-14-2026 09:15:10:123",
                "65.108.10.180",
                "75.101.130.5",
                "alice",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "POST",
                "https://example.com/api",
                "201",
                "512",
                "1024",
                "256",
                "2048",
                "https://example.com/");

        LogEntry entry = TsvParser.parseLine(line, LOG_NAME);

        assertNotNull(entry);
        assertEquals("alice", entry.getLogin());
        assertEquals("POST", entry.getMethod());
        assertEquals("https://example.com/api", entry.getUrl());
        assertEquals("https://example.com/", entry.getReferrer());
    }

    @Test
    void returnsNullForBlankLine() {
        assertNull(TsvParser.parseLine("", LOG_NAME));
        assertNull(TsvParser.parseLine("   ", LOG_NAME));
        assertNull(TsvParser.parseLine(null, LOG_NAME));
    }

    @Test
    void skipsHeaderRow() {
        String header = String.join("\t",
                "time", "cip", "sip", "login", "ua", "method", "url",
                "respcode", "reqhdrsize", "reqsize", "resphrdsize", "respsize", "referrer");
        assertNull(TsvParser.parseLine(header, LOG_NAME));
    }

    @Test
    void returnsNullForMalformedWhitespaceLine() {
        // No HTTP method present — regex anchor cannot find a boundary after the UA.
        String line = "05-14-2026 09:15:10:123 1.2.3.4 5.6.7.8 user ua-string and more text";
        assertNull(TsvParser.parseLine(line, LOG_NAME));
    }

    @Test
    void returnsNullForTabLineWithTooFewColumns() {
        String line = String.join("\t", "05-14-2026 09:15:10:123", "1.2.3.4", "5.6.7.8");
        assertNull(TsvParser.parseLine(line, LOG_NAME));
    }

    @Test
    void dashIsTreatedAsNull() {
        String line = String.join("\t",
                "05-14-2026 09:15:10:123",
                "1.2.3.4",
                "5.6.7.8",
                "-",
                "ua",
                "GET",
                "/path",
                "200",
                "100",
                "100",
                "100",
                "100",
                "-");

        LogEntry entry = TsvParser.parseLine(line, LOG_NAME);

        assertNotNull(entry);
        assertNull(entry.getLogin());
        assertNull(entry.getReferrer());
    }

    @Test
    void truncatesOversizedFields() {
        String longUa = "a".repeat(2000);
        String longUrl = "b".repeat(3000);
        String longReferrer = "c".repeat(3000);
        String line = String.join("\t",
                "05-14-2026 09:15:10:123",
                "1.2.3.4",
                "5.6.7.8",
                "user",
                longUa,
                "GET",
                longUrl,
                "200",
                "100",
                "100",
                "100",
                "100",
                longReferrer);

        LogEntry entry = TsvParser.parseLine(line, LOG_NAME);

        assertNotNull(entry);
        assertEquals(1024, entry.getUa().length());
        assertEquals(2048, entry.getUrl().length());
        assertEquals(2048, entry.getReferrer().length());
    }
}
