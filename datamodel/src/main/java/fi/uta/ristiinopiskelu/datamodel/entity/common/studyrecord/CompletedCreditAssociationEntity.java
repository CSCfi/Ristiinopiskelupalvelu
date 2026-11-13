package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditAssociation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletedCreditAssociationEntity {

    private CompletedCreditAssociationTypeEntity completedCreditAssociationType;
    private String completedCreditAssociationId;
    private String completedCreditAssociationIdentifierCode;

    public static CompletedCreditAssociationEntity from(CompletedCreditAssociation source) {
        if (source == null) {
            return null;
        }
        CompletedCreditAssociationEntity target = new CompletedCreditAssociationEntity();
        target.setCompletedCreditAssociationType(source.getCompletedCreditAssociationType() == null ? null
            : CompletedCreditAssociationTypeEntity.valueOf(source.getCompletedCreditAssociationType().name()));
        target.setCompletedCreditAssociationId(source.getCompletedCreditAssociationId());
        target.setCompletedCreditAssociationIdentifierCode(source.getCompletedCreditAssociationIdentifierCode());
        return target;
    }

    public static List<CompletedCreditAssociationEntity> from(List<CompletedCreditAssociation> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(CompletedCreditAssociationEntity::from).toList();
    }
}
