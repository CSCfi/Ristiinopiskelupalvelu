package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.AssessmentItemWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.AssessmentItemTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssessmentItemEntity {

    private String assessmentItemId;
    private String primaryCourseUnitId;
    private AssessmentItemTypeEntity type;
    private BigDecimal creditsMin = null;
    private BigDecimal creditsMax = null;
    private LocalisedStringEntity name;
    private List<CourseUnitRealisationEntity> realisations;

    public static List<AssessmentItemEntity> from(List<AssessmentItemWriteDTO> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .map(item -> {
                AssessmentItemEntity target = new AssessmentItemEntity();
                target.setAssessmentItemId(item.getAssessmentItemId());
                target.setPrimaryCourseUnitId(item.getPrimaryCourseUnitId());
                target.setType(item.getType() == null ? null : AssessmentItemTypeEntity.valueOf(item.getType().name()));
                target.setCreditsMin(item.getCreditsMin());
                target.setCreditsMax(item.getCreditsMax());
                target.setName(LocalisedStringEntity.from(item.getName()));
                target.setRealisations(CourseUnitRealisationEntity.from(item.getRealisations()));
                return target;
            }).collect(Collectors.toList());
    }
}
