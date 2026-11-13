package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.CompletionOptionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Schema(name = "CompletionOptionV9", title = "CompletionOptionV9")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletionOptionReadDTO {

    private List<AssessmentItemReadDTO> assessmentItems;
    private String completionOptionId = null;
    private String description = null;
    private LocalisedString descriptionLocalised;
    private LocalisedString name;

    public static List<CompletionOptionReadDTO> from(List<CompletionOptionEntity> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .map(option -> {
                CompletionOptionReadDTO target = new CompletionOptionReadDTO();
                target.setCompletionOptionId(option.getCompletionOptionId());
                target.setName(LocalisedString.from(option.getName()));
                target.setDescriptionLocalised(LocalisedString.from(option.getDescription()));
                target.setDescription(Optional.ofNullable(option.getDescription())
                    .map(d -> LocalisedString.getPreferredValue(LocalisedString.from(d), List.of(Language.FI, Language.EN, Language.SV)))
                    .orElse(null));
                target.setAssessmentItems(AssessmentItemReadDTO.from(option.getAssessmentItems()));
                return target;
            }).collect(Collectors.toList());
    }
}
