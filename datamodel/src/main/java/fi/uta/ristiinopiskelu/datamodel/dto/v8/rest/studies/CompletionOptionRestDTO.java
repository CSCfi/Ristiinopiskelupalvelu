package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CompletionOptionV8", title = "CompletionOptionV8")
public class CompletionOptionRestDTO {

    private List<AssessmentItemRestDTO> assessmentItems;
    private String completionOptionId = null;
    private String description = null;
    private LocalisedString name;

    public List<AssessmentItemRestDTO> getAssessmentItems() {
        return assessmentItems;
    }

    public void setAssessmentItems(List<AssessmentItemRestDTO> assessmentItems) {
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
