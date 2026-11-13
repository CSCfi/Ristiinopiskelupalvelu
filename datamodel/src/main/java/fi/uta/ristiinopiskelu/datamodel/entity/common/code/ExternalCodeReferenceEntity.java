package fi.uta.ristiinopiskelu.datamodel.entity.common.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.ExternalCodeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalCodeReferenceEntity {

    private String codeKey;
    private String codeValue;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate codeStartDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate codeEndDate;

    public static ExternalCodeReferenceEntity from(ExternalCodeReference source) {
        if (source == null) {
            return null;
        }
        ExternalCodeReferenceEntity target = new ExternalCodeReferenceEntity();
        target.setCodeKey(source.getCodeKey());
        target.setCodeValue(source.getCodeValue());
        target.setCodeStartDate(source.getCodeStartDate());
        target.setCodeEndDate(source.getCodeEndDate());
        return target;
    }

    public static List<ExternalCodeReferenceEntity> from(List<ExternalCodeReference> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(ExternalCodeReferenceEntity::from).toList();
    }
}
