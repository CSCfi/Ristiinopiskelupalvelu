package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2 
*/
public class CompletionOption {

    private List<AssessmentItem> assessmentItems;

    @JsonProperty("completionOptionId")
    private String completionOptionId = null;

    @JsonProperty("description")
    private String description = null;
    
    private LocalisedString name;

    /**
     * optional
     * @return id
    **/
    @Schema(description = "optional")
    public String getCompletionOptionId() {
      return completionOptionId;
    }

    public void setCompletionOptionId(String completionOptionId) {
      this.completionOptionId = completionOptionId;
    }

    /**
     * M2 table 4 1.4
     * @return description
    **/
    @Schema(description = "M2 table 4 1.4")
    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    /**
     * @return List<AssessmentItem> return the assessmentItems
     */
    public List<AssessmentItem> getAssessmentItems() {
        return assessmentItems;
    }

    /**
     * @param assessmentItems the assessmentItems to set
     */
    public void setAssessmentItems(List<AssessmentItem> assessmentItems) {
        this.assessmentItems = assessmentItems;
    }

    public LocalisedString getName() {
      return name;
    }

    public void setName(LocalisedString name) {
      this.name = name;
    }
}




