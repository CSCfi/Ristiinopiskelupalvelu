package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditAssessmentEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CompletedCreditAssessmentV9", title = "CompletedCreditAssessmentV9")
public class CompletedCreditAssessment {
    private GradingScale gradingScale;
    private Grade grade;
    private String egt;
    private String description;

    public GradingScale getGradingScale() {
        return gradingScale;
    }

    public void setGradingScale(GradingScale gradingScale) {
        this.gradingScale = gradingScale;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public String getEgt() {
        return egt;
    }

    public void setEgt(String egt) {
        this.egt = egt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static CompletedCreditAssessment from(CompletedCreditAssessmentEntity source) {
        if (source == null) {
            return null;
        }
        CompletedCreditAssessment target = new CompletedCreditAssessment();
        target.setGradingScale(GradingScale.from(source.getGradingScale()));
        target.setGrade(Grade.from(source.getGrade()));
        target.setEgt(source.getEgt());
        target.setDescription(source.getDescription());
        return target;
    }
}
