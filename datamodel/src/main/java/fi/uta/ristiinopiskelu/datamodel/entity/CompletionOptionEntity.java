package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CompletionOption")
public class CompletionOptionEntity {

    private List<AssessmentItemEntity> assessmentItems;
    private String completionOptionId = null;
    private String description = null;
    private LocalisedString name;

    public List<AssessmentItemEntity> getAssessmentItems() {
        return assessmentItems;
    }

    public void setAssessmentItems(List<AssessmentItemEntity> assessmentItems) {
        this.assessmentItems = assessmentItems;
    }

    public String getCompletionOptionId() {
        return completionOptionId;
    }

    public void setCompletionOptionId(String completionOptionId) {
        this.completionOptionId = completionOptionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }
}
