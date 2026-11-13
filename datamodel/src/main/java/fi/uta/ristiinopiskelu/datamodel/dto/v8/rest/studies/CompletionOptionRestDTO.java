package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(name = "CompletionOptionV8", title = "CompletionOptionV8")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletionOptionRestDTO {

    private List<AssessmentItemRestDTO> assessmentItems;
    private String completionOptionId = null;
    private String description = null;
    private LocalisedString descriptionLocalised;
    private LocalisedString name;
}
