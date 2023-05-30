package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CompletionOption")
public class CompletionOptionReadDTO {

    private List<AssessmentItemReadDTO> assessmentItems;
    private String completionOptionId = null;
    private String description = null;
    private LocalisedString name;

    public List<AssessmentItemReadDTO> getAssessmentItems() {
        return assessmentItems;
    }

    public void setAssessmentItems(List<AssessmentItemReadDTO> assessmentItems) {
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
