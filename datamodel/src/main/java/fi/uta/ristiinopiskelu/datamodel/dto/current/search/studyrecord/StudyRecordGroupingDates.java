package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import java.time.LocalDate;

public class StudyRecordGroupingDates {

    private LocalDate start;
    private LocalDate end;

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
