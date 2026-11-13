package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditTarget;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletedCreditTargetEntity {

    private CompletedCreditTargetTypeEntity completedCreditTargetType;
    private String completedCreditTargetId;
    private String completedCreditTargetIdentifierCode;

    public static CompletedCreditTargetEntity from(CompletedCreditTarget source) {
        if (source == null) {
            return null;
        }
        CompletedCreditTargetEntity target = new CompletedCreditTargetEntity();
        target.setCompletedCreditTargetType(source.getCompletedCreditTargetType() == null ? null
            : CompletedCreditTargetTypeEntity.valueOf(source.getCompletedCreditTargetType().name()));
        target.setCompletedCreditTargetId(source.getCompletedCreditTargetId());
        target.setCompletedCreditTargetIdentifierCode(source.getCompletedCreditTargetIdentifierCode());
        return target;
    }
}
