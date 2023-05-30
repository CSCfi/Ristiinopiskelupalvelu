package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

public class StudyRightStatus  {

    @Field(type = FieldType.Integer)
    private StudyRightStatusValue studyRightStatusValue;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    public StudyRightStatusValue getStudyRightStatusValue() {
        return studyRightStatusValue;
    }

    public void setStudyRightStatusValue(StudyRightStatusValue studyRightStatusValue) {
        this.studyRightStatusValue = studyRightStatusValue;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
