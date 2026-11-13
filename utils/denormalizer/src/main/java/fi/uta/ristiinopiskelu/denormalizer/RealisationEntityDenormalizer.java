package fi.uta.ristiinopiskelu.denormalizer;

import fi.uta.ristiinopiskelu.datamodel.entity.AssessmentItemEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitRealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementTypeEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class RealisationEntityDenormalizer implements Denormalizer<RealisationEntity> {

    private static final Logger logger = LoggerFactory.getLogger(RealisationEntityDenormalizer.class);

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Override
    public void denormalize() {
        List<RealisationEntity> realisationEntities = StreamSupport.stream(realisationRepository.findAll(Pageable.unpaged()).spliterator(), false)
            .collect(Collectors.toList());

        for(RealisationEntity realisationEntity : realisationEntities) {
            this.addOrUpdateDenormalizedData(realisationEntity, realisationEntity.getStudyElementReferences());
        }
    }

    private void addOrUpdateDenormalizedData(RealisationEntity realisationToDenormalize, List<StudyElementReferenceEntity> references) {
        if(CollectionUtils.isEmpty(references)) {
            return;
        }

        for(StudyElementReferenceEntity reference : references) {
            CourseUnitEntity referencedCourseUnit = getReferencedCourseUnitEntity(reference);

            // This should be validated before calling and should not happen. Cannot throw exception here since it would
            // cause returning failure message for message sending organisation even though realisations have already been saved
            if(referencedCourseUnit == null) {
                logger.error("addOrUpdateDenormalizedData called with realisation that has reference to course unit that does not exist!" +
                    " this should never happen. Realisation references must be validated before calling addOrUpdateDenormalizedData.");
                continue;
            }

            CourseUnitRealisationEntity courseUnitRealisationEntity = toCourseUnitRealisationEntity(realisationToDenormalize);
            addOrUpdateCourseUnitRealisation(reference, referencedCourseUnit, courseUnitRealisationEntity);
            courseUnitRepository.update(referencedCourseUnit);
        }
    }


    private void addOrUpdateCourseUnitRealisation(StudyElementReferenceEntity reference, CourseUnitEntity referencedCourseUnit,
                                                  CourseUnitRealisationEntity newCourseUnitRealisationEntity) {
        Predicate<CourseUnitRealisationEntity> referenceMatchesToDenormalizedData =
            cur -> cur.getRealisationId().equals(newCourseUnitRealisationEntity.getRealisationId())
                && cur.getOrganizingOrganisationId().equals(newCourseUnitRealisationEntity.getOrganizingOrganisationId());

        if(reference.getReferenceType() == StudyElementTypeEntity.ASSESSMENT_ITEM) {
            for(AssessmentItemEntity assessmentItemEntity : referencedCourseUnit.getAssessmentItems()) {
                if(assessmentItemEntity.getAssessmentItemId().equals(reference.getReferenceAssessmentItemId())) {
                    if(assessmentItemEntity.getRealisations() == null) {
                        assessmentItemEntity.setRealisations(new ArrayList<>());
                    }
                    assessmentItemEntity.getRealisations().removeIf(referenceMatchesToDenormalizedData);
                    assessmentItemEntity.getRealisations().add(newCourseUnitRealisationEntity);

                    logger.info("Redenormalized courseUnit [studyElementId={}, organizingOrganisationId={}] assessmentItemRealisation " +
                        "[realisationId={}, organizingOrganisationId={}",
                        referencedCourseUnit.getStudyElementId(), referencedCourseUnit.getOrganizingOrganisationId(),
                        newCourseUnitRealisationEntity.getRealisationId(), newCourseUnitRealisationEntity.getOrganizingOrganisationId());
                }
            }
        } else {
            if(referencedCourseUnit.getRealisations() == null) {
                referencedCourseUnit.setRealisations(new ArrayList<>());
            }
            referencedCourseUnit.getRealisations().removeIf(referenceMatchesToDenormalizedData);
            referencedCourseUnit.getRealisations().add(newCourseUnitRealisationEntity);

            logger.info("Redenormalized courseUnit [studyElementId={}, organizingOrganisationId={}] realisation " +
                    "[realisationId={}, organizingOrganisationId={}",
                referencedCourseUnit.getStudyElementId(), referencedCourseUnit.getOrganizingOrganisationId(),
                newCourseUnitRealisationEntity.getRealisationId(), newCourseUnitRealisationEntity.getOrganizingOrganisationId());
        }
    }

    private CourseUnitEntity getReferencedCourseUnitEntity(StudyElementReferenceEntity reference) {
        if(reference.getReferenceType() == StudyElementTypeEntity.ASSESSMENT_ITEM) {
            return courseUnitRepository.findByIdAndAssessmentItemIdAndOrganizer(
                reference.getReferenceIdentifier(), reference.getReferenceAssessmentItemId(), reference.getReferenceOrganizer())
                .orElse(null);
        } else {
            return courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
                reference.getReferenceIdentifier(), reference.getReferenceOrganizer())
                .orElse(null);
        }
    }

    private CourseUnitRealisationEntity toCourseUnitRealisationEntity(RealisationEntity source) {
        CourseUnitRealisationEntity target = new CourseUnitRealisationEntity();
        target.setRealisationId(source.getRealisationId());
        target.setRealisationIdentifierCode(source.getRealisationIdentifierCode());
        target.setOrganizingOrganisationId(source.getOrganizingOrganisationId());
        target.setName(source.getName());
        target.setEnrollmentStartDateTime(source.getEnrollmentStartDateTime());
        target.setEnrollmentEndDateTime(source.getEnrollmentEndDateTime());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setEnrollmentClosed(source.isEnrollmentClosed());
        target.setCooperationNetworks(source.getCooperationNetworks());
        target.setTeachingLanguage(source.getTeachingLanguage());
        target.setStatus(source.getStatus());
        target.setMinEduGuidanceArea(source.getMinEduGuidanceArea());
        return target;
    }
}
