package rg.financialplanning.web.health;

import org.junit.Test;

import static org.junit.Assert.*;

public class HealthPremiumEstimateParserTest {

    @Test
    public void takesTheLastJsonObjectAndKeepsTheProseAsNotes() {
        String out = "## Findings\n\nThe benchmark is about $1,450 ([KFF](https://kff.org/x)).\n"
                + "Example shape: {\"preMedicarePremiumMonthly\": 1}\n\n"
                + "{\"preMedicarePremiumMonthly\": 1450, \"medicarePlanPremiumMonthly\": 235.5, "
                + "\"medicalInflationPct\": 6.0, \"sources\": [\"https://kff.org/x\", \"https://cms.gov/y\"]}";
        HealthPremiumEstimateParser.Parsed p = HealthPremiumEstimateParser.parse(out);
        assertEquals(1450, p.preMedicareMonthly(), 0);
        assertEquals(235.5, p.medicarePlanMonthly(), 0);
        assertEquals(6.0, p.medicalInflationPct(), 0);
        assertEquals(2, p.sources().size());
        assertTrue(p.notes().startsWith("## Findings"));
        assertTrue(p.notes().endsWith("{\"preMedicarePremiumMonthly\": 1}"));
    }

    @Test
    public void toleratesACodeFenceAndStringNumbers() {
        String out = "Text.\n```json\n{\"preMedicarePremiumMonthly\": \"$1,500\", \"medicarePlanPremiumMonthly\": \"240\", "
                + "\"medicalInflationPct\": \"5.5%\", \"sources\": []}\n```";
        HealthPremiumEstimateParser.Parsed p = HealthPremiumEstimateParser.parse(out);
        assertEquals(1500, p.preMedicareMonthly(), 0);
        assertEquals(240, p.medicarePlanMonthly(), 0);
        assertEquals(5.5, p.medicalInflationPct(), 0);
        assertEquals("Text.", p.notes());
    }

    @Test
    public void bracesInsideTheProseDoNotConfuseTheMatch() {
        String out = "A set {a, b} and an object {\"x\": {\"y\": 1}} appear. "
                + "{\"preMedicarePremiumMonthly\": 0, \"medicarePlanPremiumMonthly\": 200, \"medicalInflationPct\": 6}";
        HealthPremiumEstimateParser.Parsed p = HealthPremiumEstimateParser.parse(out);
        assertEquals(200, p.medicarePlanMonthly(), 0);
        assertTrue(p.sources().isEmpty());
    }

    @Test
    public void workingMonologueBeforeTheFirstHeadingIsDropped() {
        String out = "All lookups are done, so I can finish the write-up.\n\n## Figures\n\nText.\n\n"
                + "{\"preMedicarePremiumMonthly\": 1, \"medicarePlanPremiumMonthly\": 2, \"medicalInflationPct\": 3}";
        assertEquals("## Figures\n\nText.", HealthPremiumEstimateParser.parse(out).notes());
        assertEquals("no heading at all", HealthPremiumEstimateParser.fromFirstHeading("no heading at all"));
    }

    @Test
    public void noFiguresIsAnError() {
        try {
            HealthPremiumEstimateParser.parse("I could not find the data. {\"foo\": 1}");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("no JSON object"));
        }
    }
}
