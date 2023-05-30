package fi.uta.ristiinopiskelu.persistence.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemesterDatePeriod {

    private static final Logger logger = LoggerFactory.getLogger(SemesterDatePeriod.class);

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ZoneId zoneId;

    public SemesterDatePeriod(OffsetDateTime startDate, OffsetDateTime endDate) {
        this.startDate = startDate.toLocalDate();
        this.endDate = endDate.toLocalDate();
        this.zoneId = resolveZoneId();
    }

    public SemesterDatePeriod(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.zoneId = resolveZoneId();
    }

    private ZoneId resolveZoneId() {
        try {
            return ZoneId.of("Europe/Helsinki");
        } catch (Exception e) {
            logger.error("Error while attempting to create ZoneId of 'Europe/Helsinki', falling back to system default '{}'", ZoneId.systemDefault(), e);
        }

        return ZoneId.systemDefault();
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public boolean contains(LocalDate date) {
        return DateUtils.isBeforeOrEqual(startDate, date) && DateUtils.isAfterOrEqual(endDate, date);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public OffsetDateTime getStartDateAsOffset() {
        return getStartDateAsZoned().toOffsetDateTime();
    }

    public OffsetDateTime getEndDateAsOffset() {
        return getEndDateAsZoned().toOffsetDateTime();
    }

    public ZonedDateTime getStartDateAsZoned() {
        return ZonedDateTime.of(startDate, LocalTime.of(0, 0, 0, 0), getZoneId());
    }

    public ZonedDateTime getEndDateAsZoned() {
        return ZonedDateTime.of(endDate, LocalTime.of(23, 59, 59, 999999999), getZoneId());
    }

    /**
     * Gets a semester start date determined by a position in time starting from given date. A semesterPosition value of 0 would give you
     * the current semester date period, A value of 1 would give you the next semester date period etc.
     *
     * Semesters in Finland as of January 2020:
     * Spring semester: 1.1.-31.7.
     * Fall semester: 1.8.-31.12.
     *
     * @param now current date
     * @param semesterPosition position in time of the wanted semester starting from 0
     * @return semester date period
     */
    public static SemesterDatePeriod getSemesterDatePeriod(LocalDate now, int semesterPosition) {
        Assert.notNull(now, "LocalDate 'now' cannot be null");
        Assert.isTrue(semesterPosition >= 0, "Semester position must not be negative");

        Map<Integer, SemesterDatePeriod> periods = new HashMap<>();

        int year = now.getYear();
        for(int i = 0; i < semesterPosition + 2; i += 2) {
            periods.put(i, new SemesterDatePeriod(LocalDate.of(year, 1, 1), LocalDate.of(year, 7, YearMonth.of(year, 7).lengthOfMonth())));
            periods.put(i + 1, new SemesterDatePeriod(LocalDate.of(year, 8, 1), LocalDate.of(year, 12, YearMonth.of(year, 12).lengthOfMonth())));
            year++;
        }

        int currentPeriodKey = periods.entrySet().stream().filter(entry -> entry.getValue().contains(now)).findFirst().get().getKey();
        return periods.get(currentPeriodKey + semesterPosition);
    }

    public static List<SemesterDatePeriod> getSemesterDatePeriodsForRange(LocalDate startDate, LocalDate endDate) {
        List<SemesterDatePeriod> periods = new ArrayList<>();

        int currentSemesterPosition = 0;

        SemesterDatePeriod currentSemesterDatePeriod = SemesterDatePeriod.getSemesterDatePeriod(startDate, currentSemesterPosition);
        periods.add(currentSemesterDatePeriod);

        while(endDate.isAfter(currentSemesterDatePeriod.getEndDate())) {
            currentSemesterPosition++;
            currentSemesterDatePeriod = SemesterDatePeriod.getSemesterDatePeriod(startDate, currentSemesterPosition);
            periods.add(currentSemesterDatePeriod);
        }

        return periods;
    }
}
