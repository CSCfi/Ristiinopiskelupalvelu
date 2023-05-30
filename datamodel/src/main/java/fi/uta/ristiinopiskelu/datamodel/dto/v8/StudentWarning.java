package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

public class StudentWarning {

    private StudentWarningType type;
    private LocalisedString description;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;
    
    private String studyRightId;

    public StudentWarningType getType() {
        return type;
    }

    public void setType(StudentWarningType type) {
        this.type = type;
    }

    public LocalisedString getDescription() {
        return description;
    }

    public void setDescription(LocalisedString description) {
        this.description = description;
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

    public String getStudyRightId() {
        return studyRightId;
    }

    public void setStudyRightId(String studyRightId) {
        this.studyRightId = studyRightId;
    }
}
