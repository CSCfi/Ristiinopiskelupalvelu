package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code;

import fi.uta.ristiinopiskelu.datamodel.entity.common.code.ExternalCodeReferenceEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "ExternalCodeReferenceV9", title = "ExternalCodeReferenceV9")
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

    public static ExternalCodeReference from(ExternalCodeReferenceEntity source) {
        if (source == null) {
            return null;
        }
        ExternalCodeReference target = new ExternalCodeReference();
        target.setCodeKey(source.getCodeKey());
        target.setCodeValue(source.getCodeValue());
        target.setCodeStartDate(source.getCodeStartDate());
        target.setCodeEndDate(source.getCodeEndDate());
        return target;
    }

    public static List<ExternalCodeReference> from(List<ExternalCodeReferenceEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(ExternalCodeReference::from).toList();
    }
}
