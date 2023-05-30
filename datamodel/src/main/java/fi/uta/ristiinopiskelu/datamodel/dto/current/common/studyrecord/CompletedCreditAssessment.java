package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

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
}
