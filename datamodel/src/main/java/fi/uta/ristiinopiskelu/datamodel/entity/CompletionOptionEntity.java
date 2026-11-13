package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CompletionOptionWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletionOptionEntity {

    private List<AssessmentItemEntity> assessmentItems;
    private String completionOptionId = null;
    private LocalisedStringEntity description = null;
    private LocalisedStringEntity name;

    public static List<CompletionOptionEntity> from(List<CompletionOptionWriteDTO> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .map(option -> {
                CompletionOptionEntity target = new CompletionOptionEntity();
                target.setCompletionOptionId(option.getCompletionOptionId());
                target.setName(LocalisedStringEntity.from(option.getName()));
                target.setDescription(StringUtils.hasText(option.getDescription())
                    ? LocalisedStringEntity.from(new LocalisedString(option.getDescription()))
                    : LocalisedStringEntity.from(option.getDescriptionLocalised()));
                target.setAssessmentItems(AssessmentItemEntity.from(option.getAssessmentItems()));
                return target;
            }).collect(Collectors.toList());
    }
}
