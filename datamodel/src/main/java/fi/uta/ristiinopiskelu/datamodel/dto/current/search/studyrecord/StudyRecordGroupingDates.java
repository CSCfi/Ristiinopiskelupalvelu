package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "StudyRecordGroupingDatesV9", title = "StudyRecordGroupingDatesV9")
public class StudyRecordGroupingDates {

    private LocalDate start;
    private LocalDate end;

    public StudyRecordGroupingDates() {
    }

    public StudyRecordGroupingDates(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }
}
