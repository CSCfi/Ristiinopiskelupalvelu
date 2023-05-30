package fi.uta.ristiinopiskelu.handler.utils;

import fi.uta.ristiinopiskelu.persistence.utils.SemesterDatePeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SemesterDatePeriodTest {

    @Test
    public void testGetSemesterDatePeriod_withMultipleDatesAndSemesterPositions_shouldSucceed() {
        SemesterDatePeriod period = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.of(2018, 9, 25), 0);
        assertEquals(period.getStartDate(), LocalDate.of(2018, 8, 1));

        period = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.of(2019, 4, 25), 1);
        assertEquals(period.getStartDate(), LocalDate.of(2019, 8, 1));

        period = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.of(2019, 9, 25), 2);
        assertEquals(period.getStartDate(), LocalDate.of(2020, 8, 1));

        period = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.of(2018, 10, 4), 5);
        assertEquals(period.getStartDate(), LocalDate.of(2021, 1, 1));

        period = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.of(2018, 7, 25), 4);
        assertEquals(period.getStartDate(), LocalDate.of(2020, 1, 1));
    }

    @Test
    public void testGetSemesterDatePeriod_forLongRange_shouldSucceed() {
        LocalDate startDate = LocalDate.of(2018, 5, 1);
        LocalDate endDate = LocalDate.of(2022, 9, 15);

        /* should be
        - 2017-2018
        - 2018-2019
        - 2019-2020
        - 2020-2021
        - 2021-2022
        - 2022-2023
         */
        List<SemesterDatePeriod> periodsDuringDate = SemesterDatePeriod.getSemesterDatePeriodsForRange(startDate, endDate);
        assertEquals(10, periodsDuringDate.size());
    }

    @Test
    public void testDatePeriod_timeZoneOffSetCorrectlyInSummerTime_shouldSucceed() {
        SemesterDatePeriod period = new SemesterDatePeriod(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 12, 1));

        OffsetDateTime startDate = period.getStartDateAsOffset();
        assertEquals("+03:00", startDate.getOffset().toString());

        OffsetDateTime endDate = period.getEndDateAsOffset();
        assertEquals("+02:00", endDate.getOffset().toString());
    }

    @Test
    public void testFormattingDoesNotGuaranteeCorrectOffsetDateTimeFormat() {
        String PATTERN = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX";
        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);
        OffsetDateTime time = OffsetDateTime.of(2021, 1, 1, 22, 0, 0, 0, OffsetDateTime.now().getOffset());
        assertNotEquals("2021-01-01T22:00:00.000+02:00", OffsetDateTime.parse(FORMATTER.format(time)).toString());
    }
}
