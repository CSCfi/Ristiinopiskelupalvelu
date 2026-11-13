package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditAssessment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletedCreditAssessmentEntity {

    private GradingScaleEntity gradingScale;
    private GradeEntity grade;
    private String egt;
    private String description;

    public static CompletedCreditAssessmentEntity from(CompletedCreditAssessment source) {
        if (source == null) {
            return null;
        }
        CompletedCreditAssessmentEntity target = new CompletedCreditAssessmentEntity();
        target.setGradingScale(GradingScaleEntity.from(source.getGradingScale()));
        target.setGrade(GradeEntity.from(source.getGrade()));
        target.setEgt(source.getEgt());
        target.setDescription(source.getDescription());
        return target;
    }
}
