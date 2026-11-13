package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.PersonEntity;
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
public class CompletedCreditEntity {

    private String educationInstitution;
    private String completedCreditIdentifier;
    private CompletedCreditTargetEntity completedCreditTarget;
    private List<CompletedCreditAssociationEntity> completedCreditAssociations;
    private List<String> assessmentItemRealisationsOfMethodOfCompletion;
    private LocalisedStringEntity completedCreditName;
    private LocalisedStringEntity completedCreditObjective;
    private LocalisedStringEntity completedCreditContent;
    private CompletedCreditStatusEntity completedCreditStatus;
    private Double scope;
    private CompletedCreditAssessmentEntity assessment;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate completionDate;

    private List<String> languagesOfCompletion;
    private List<PersonEntity> acceptors;
    private StudyRecordOrganisationEntity organisationResponsibleForCompletion;
    private CompletedCreditTransactionEntity transaction;

    @Field(type = FieldType.Integer)
    private CompletedCreditTypeEntity type;

    @Field(type = FieldType.Integer)
    private MinEduGuidanceAreaEntity minEduGuidanceArea;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate assessmentDate;

    public static CompletedCreditEntity from(CompletedCredit source) {
        if (source == null) {
            return null;
        }

        CompletedCreditEntity target = new CompletedCreditEntity();
        target.setEducationInstitution(source.getEducationInstitution());
        target.setCompletedCreditIdentifier(source.getCompletedCreditIdentifier());
        target.setCompletedCreditTarget(CompletedCreditTargetEntity.from(source.getCompletedCreditTarget()));
        target.setCompletedCreditAssociations(CompletedCreditAssociationEntity.from(source.getCompletedCreditAssociations()));
        target.setAssessmentItemRealisationsOfMethodOfCompletion(source.getAssessmentItemRealisationsOfMethodOfCompletion());
        target.setCompletedCreditName(LocalisedStringEntity.from(source.getCompletedCreditName()));
        target.setCompletedCreditObjective(LocalisedStringEntity.from(source.getCompletedCreditObjective()));
        target.setCompletedCreditContent(LocalisedStringEntity.from(source.getCompletedCreditContent()));
        target.setCompletedCreditStatus(source.getCompletedCreditStatus() == null ? null
            : CompletedCreditStatusEntity.valueOf(source.getCompletedCreditStatus().name()));
        target.setScope(source.getScope());
        target.setAssessment(CompletedCreditAssessmentEntity.from(source.getAssessment()));
        target.setCompletionDate(source.getCompletionDate());
        target.setLanguagesOfCompletion(source.getLanguagesOfCompletion());
        target.setAcceptors(PersonEntity.from(source.getAcceptors()));
        target.setOrganisationResponsibleForCompletion(StudyRecordOrganisationEntity.from(source.getOrganisationResponsibleForCompletion()));
        target.setTransaction(source.getTransaction() == null ? null : CompletedCreditTransactionEntity.valueOf(source.getTransaction().name()));
        target.setType(source.getType() == null ? null : CompletedCreditTypeEntity.fromValue(source.getType().getCode()));
        target.setMinEduGuidanceArea(source.getMinEduGuidanceArea() == null ? null
            : MinEduGuidanceAreaEntity.fromValue(source.getMinEduGuidanceArea().getCode()));
        target.setAssessmentDate(source.getAssessmentDate());
        return target;
    }

    public static List<CompletedCreditEntity> from(List<CompletedCredit> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(CompletedCreditEntity::from).toList();
    }
}
