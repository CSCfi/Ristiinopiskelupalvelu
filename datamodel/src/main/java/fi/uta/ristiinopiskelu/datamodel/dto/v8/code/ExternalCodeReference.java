package fi.uta.ristiinopiskelu.datamodel.dto.v8.code;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

public class ExternalCodeReference {

    private String codeKey;
    private String codeValue;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate codeStartDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate codeEndDate;

    public String getCodeKey() {
        return codeKey;
    }

    public void setCodeKey(String codeKey) {
        this.codeKey = codeKey;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public LocalDate getCodeStartDate() {
        return codeStartDate;
    }

    public void setCodeStartDate(LocalDate codeStartDate) {
        this.codeStartDate = codeStartDate;
    }

    public LocalDate getCodeEndDate() {
        return codeEndDate;
    }

    public void setCodeEndDate(LocalDate codeEndDate) {
        this.codeEndDate = codeEndDate;
    }
}
