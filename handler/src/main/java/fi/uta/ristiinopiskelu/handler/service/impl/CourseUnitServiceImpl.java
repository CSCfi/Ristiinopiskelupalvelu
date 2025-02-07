package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.AssessmentItemWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.AssessmentItemEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitRealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.DeleteFailedException;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyElementRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class CourseUnitServiceImpl extends AbstractStudyElementService<CourseUnitWriteDTO, CourseUnitEntity, CourseUnitReadDTO> implements CourseUnitService {

    private static final Logger logger = LoggerFactory.getLogger(CourseUnitServiceImpl.class);

    private CourseUnitRepository courseUnitRepository;
    private RealisationRepository realisationRepository;
    private ModelMapper modelMapper;

    @Autowired
    public CourseUnitServiceImpl(CourseUnitRepository courseUnitRepository, RealisationRepository realisationRepository, ModelMapper modelMapper) {
        super(CourseUnitWriteDTO.class, CourseUnitEntity.class, CourseUnitReadDTO.class, StudyElementType.COURSE_UNIT);
        this.courseUnitRepository = courseUnitRepository;
        this.realisationRepository = realisationRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    protected StudyElementRepository<CourseUnitEntity> getRepository() {
        return courseUnitRepository;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public Optional<CourseUnitEntity> findByIdAndAssessmentItemIdAndOrganizer(String id, String assessmentItemId, String organizingOrganisationId) {
        Assert.hasText(id, "StudyElementId cannot be empty");
        Assert.hasText(assessmentItemId, "AssessmentItemId cannot be empty");
        Assert.hasText(organizingOrganisationId, "OrganizingOrganisationId id cannot be empty");
        return courseUnitRepository.findByIdAndAssessmentItemIdAndOrganizer(id, assessmentItemId, organizingOrganisationId);
    }

    @Override
    public List<CompositeIdentifiedEntityModificationResult> create(CourseUnitEntity entity) {
        if(!CollectionUtils.isEmpty(entity.getRealisations())) {
            entity.getRealisations().forEach(
                cur -> cur.setOrganizingOrganisationId(entity.getOrganizingOrganisationId()));
        }

        if(!CollectionUtils.isEmpty(entity.getAssessmentItemRealisations())) {
            entity.getAssessmentItemRealisations().forEach(
                cur -> cur.setOrganizingOrganisationId(entity.getOrganizingOrganisationId()));
        }

        return super.create(entity);
    }

    @Override
    public List<CompositeIdentifiedEntityModificationResult> createAll(List<CourseUnitWriteDTO> courseUnits, String organisationId) throws CreateFailedException {
        List<String> createdRealisationHistoryIds = new ArrayList<>();
        List<CompositeIdentifiedEntityModificationResult> modificationResults = new ArrayList<>();

        try {
            for (CourseUnitWriteDTO courseUnit : courseUnits) {

                CourseUnitEntity existing = getRepository().findByStudyElementIdAndOrganizingOrganisationId(
                        courseUnit.getStudyElementId(), organisationId).orElse(null);

                if(existing != null) {
                    throw new IllegalStateException("Course unit already exists with id " + existing.getId());
                }

                StudyElementReference courseUnitReference = new StudyElementReference(courseUnit.getStudyElementId(), organisationId, StudyElementType.COURSE_UNIT);

                if(!CollectionUtils.isEmpty(courseUnit.getRealisations())) {
                    modificationResults.addAll(createOrUpdateRealisations(courseUnit.getRealisations(), courseUnitReference, organisationId, createdRealisationHistoryIds));
                }

                for(AssessmentItemWriteDTO assessmentItem : courseUnit.getAssessmentItems()) {
                    StudyElementReference assessmentItemReference = new StudyElementReference(courseUnit.getStudyElementId(),
                            organisationId, StudyElementType.ASSESSMENT_ITEM, assessmentItem.getAssessmentItemId());

                    if(!CollectionUtils.isEmpty(assessmentItem.getRealisations())) {
                        modificationResults.addAll(createOrUpdateRealisations(assessmentItem.getRealisations(), assessmentItemReference,
                                organisationId, createdRealisationHistoryIds));
                    }
                }

                CourseUnitEntity courseUnitEntity = modelMapper.map(courseUnit, CourseUnitEntity.class);
                courseUnitEntity.setType(CompositeIdentifiedEntityType.COURSE_UNIT);
                modificationResults.addAll(this.create(courseUnitEntity));
            }
        } catch(Exception e) {
            rollback(modificationResults, createdRealisationHistoryIds);
            throw new CreateFailedException(getEntityClass(), e);
        }

        return modificationResults;
    }

    @Override
    public List<CompositeIdentifiedEntityModificationResult> update(JsonNode json, String organisationId) throws UpdateFailedException {
        Assert.notNull(json.get("studyElementId"), "Json must have studyElementId field");
        Assert.hasText(organisationId, "Missing organisation JMS header");

        // Force removing of realisations in case they are filled since we do not want to update denormalized realisations with course unit
        // TODO: Create specific UpdateCourseUnitRequestDTO without realisations so there cannot exists these realisations
        if(json.has("realisations")) {
            ((ObjectNode)json).remove("realisations");
        }

        if(json.hasNonNull("completionOptions") && json.get("completionOptions").isArray()) {
            for(JsonNode completionOptionNode : json.get("completionOptions")) {
                if(completionOptionNode.hasNonNull("assessmentItems") && completionOptionNode.get("assessmentItems").isArray()) {
                    for(JsonNode assessmentItemNode : completionOptionNode.get("assessmentItems")) {
                        ((ObjectNode)assessmentItemNode).remove("realisations");
                    }
                }
            }
        }

        String studyElementId = json.get("studyElementId").asText();
        List<CompositeIdentifiedEntityModificationResult> results = super.update(json, organisationId);
        CompositeIdentifiedEntityModificationResult courseUnitModificationResult = results.stream()
            .filter(r -> r.getCurrent() != null && (r.getCurrent().getElementId().equals(studyElementId) && r.getCurrent().getOrganizingOrganisationId().equals(organisationId)))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Can not find updated course unit"));

        CourseUnitEntity updatedCourseUnit = (CourseUnitEntity) courseUnitModificationResult.getCurrent();

        // If assessment items were updated, update denormalized realisation data since it was replaced
        if(json.hasNonNull("completionOptions")) {
            for(AssessmentItemEntity assessmentItemEntity : updatedCourseUnit.getAssessmentItems()) {
                List<RealisationEntity> realisationEntity = realisationRepository.findByAssessmentItemReference(updatedCourseUnit.getStudyElementId(),
                    updatedCourseUnit.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

                if(!CollectionUtils.isEmpty(realisationEntity)) {
                    assessmentItemEntity.setRealisations(realisationEntity.stream()
                        .map(r -> modelMapper.map(r, CourseUnitRealisationEntity.class))
                        .collect(Collectors.toList()));
                }
            }

            updatedCourseUnit = courseUnitRepository.update(updatedCourseUnit);
            courseUnitModificationResult.setCurrent(updatedCourseUnit);
        }

        return results;
    }

    @Override
    public List<CompositeIdentifiedEntityModificationResult> delete(String studyElementId, String organizingOrganisationId, boolean deleteRealisations) throws DeleteFailedException {
        List<CompositeIdentifiedEntityModificationResult> modificationResults = new ArrayList<>();

        List<RealisationEntity> realisations = realisationRepository.findByStudyElementReference(
                studyElementId, organizingOrganisationId, RealisationEntity.class);

        if(!CollectionUtils.isEmpty(realisations)) {
            if(deleteRealisations) {
                for(RealisationEntity realisation : realisations) {
                    // if realisation has reference only to course unit that will be deleted, delete realisation also
                    if(realisation.getStudyElementReferences().size() == 1) {
                        realisationRepository.saveHistory(realisation, RealisationEntity.class);
                        realisationRepository.deleteById(realisation.getId());
                        modificationResults.add(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.DELETE, realisation.getType(), realisation, null));
                    } else {
                        Predicate<StudyElementReference> matches =
                                ser -> ser.getReferenceIdentifier().equals(studyElementId) &&
                                ser.getReferenceOrganizer().equals(organizingOrganisationId);

                        realisationRepository.saveHistory(realisation, RealisationEntity.class);

                        RealisationEntity updated = realisationRepository.findByRealisationIdAndOrganizingOrganisationId(realisation.getRealisationId(), realisation.getOrganizingOrganisationId())
                            .orElseThrow(() -> new EntityNotFoundException("Realisation not found with realisationId %s and organizingOrganisationId %s".formatted(realisation.getRealisationId(), realisation.getOrganizingOrganisationId())));
                        updated.getStudyElementReferences().removeIf(matches);
                        updated = realisationRepository.update(updated);
                        modificationResults.add(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.UPDATE, realisation.getType(), realisation, updated));
                    }
                }
            } else {
                throw new DeleteFailedException("Course unit cannot be deleted if it has realisations and deleteRealisations is false.");
            }
        }

        try {
            CourseUnitEntity courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
                studyElementId, organizingOrganisationId).orElseThrow(() -> new EntityNotFoundException(CourseUnitEntity.class, studyElementId));

            courseUnitRepository.saveHistory(courseUnitEntity, CourseUnitEntity.class);
            courseUnitRepository.delete(courseUnitEntity);

            modificationResults.add(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.DELETE, courseUnitEntity.getType(), courseUnitEntity, null));
            return modificationResults;
        } catch (Exception e) {
            throw new DeleteFailedException(getEntityClass(), e);
        }
    }

    private List<CompositeIdentifiedEntityModificationResult> createOrUpdateRealisations(List<RealisationWriteDTO> realisations, StudyElementReference reference,
                                                                                         String organisationId, List<String> createdRealisationHistoryIds) {
        List<CompositeIdentifiedEntityModificationResult> results = new ArrayList<>();

        for (RealisationWriteDTO realisation : realisations) {
            RealisationEntity updated = realisationRepository.findByRealisationIdAndOrganizingOrganisationId(
                    realisation.getRealisationId(), organisationId)
                    .orElse(null);

            // check if reference is okay on the realisation, if not, add it.
            if (updated != null) {
                createdRealisationHistoryIds.add(realisationRepository.saveHistory(updated, RealisationEntity.class));
                RealisationEntity original = copy(updated, RealisationEntity.class);
                updated = checkReferences(updated, reference);
                updated = realisationRepository.update(updated);
                
                results.add(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.UPDATE, CompositeIdentifiedEntityType.REALISATION, original, updated));
            } else {
                // add organizingOrganisationId
                RealisationEntity realisationEntity = modelMapper.map(realisation, RealisationEntity.class);
                realisationEntity.setOrganizingOrganisationId(organisationId);
                realisationEntity.setStatus(realisation.getStatus() != null ? realisation.getStatus() : StudyStatus.ACTIVE);

                realisationEntity = checkReferences(realisationEntity, reference);
                RealisationEntity created = realisationRepository.create(realisationEntity);

                results.add(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.CREATE, CompositeIdentifiedEntityType.REALISATION, null, created));
            }
        }

        return results;
    }

    private RealisationEntity checkReferences(RealisationEntity realisationEntity, StudyElementReference reference) {
        if (!CollectionUtils.isEmpty(realisationEntity.getStudyElementReferences())) {
            if (!realisationEntity.getStudyElementReferences().contains(reference)) {
                realisationEntity.getStudyElementReferences().add(reference);
            }
        } else {
            realisationEntity.setStudyElementReferences(Collections.singletonList(reference));
        }

        return realisationEntity;
    }

    private void rollback(List<CompositeIdentifiedEntityModificationResult> results, List<String> createdRealisationHistoryIds) {
        for (CompositeIdentifiedEntityModificationResult created : results.stream().filter(result -> result.getOperationType() == ModificationOperationType.CREATE).toList()) {
            CompositeIdentifiedEntity current = created.getCurrent();
            logger.info("Deleting already persisted {} with id {}", current.getType(), current.getElementId());
            try {
                switch(current.getType()) {
                    case REALISATION: realisationRepository.deleteById(current.getId());
                    case COURSE_UNIT: courseUnitRepository.deleteById(current.getId());
                    default: throw new IllegalStateException("Found created element with type " + current.getType() + ", this should not happen");
                }
            } catch(Exception e) {
                logger.error("Error while attempting to delete {} with id {}", current.getType(), current.getElementId(), e);
            }
        }

        for(String id : createdRealisationHistoryIds) {
            logger.info("Removing history documents for updated realisation. History id {}", id);
            realisationRepository.deleteHistoryById(id, RealisationEntity.class);
        }

        for(CompositeIdentifiedEntityModificationResult updated : results.stream().filter(result -> result.getOperationType() == ModificationOperationType.UPDATE &&
            result.getType() == CompositeIdentifiedEntityType.REALISATION).toList()) {
            OffsetDateTime originalUpdateTime = updated.getPrevious().getUpdateTime();
            RealisationEntity realisation = (RealisationEntity) updated.getCurrent();

            logger.info("Removing reference from updated realisation {}", realisation.getId());

            if(CollectionUtils.isEmpty(realisation.getStudyElementReferences())) {
                continue;
            }

            try {
                // note: this returns references other than the one that matches
                List<StudyElementReference> validReferences = realisation.getStudyElementReferences().stream().filter(sre -> {
                    Optional<CourseUnitEntity> courseUnitEntity = this.findByStudyElementIdAndOrganizingOrganisationId(
                            sre.getReferenceIdentifier(), sre.getReferenceOrganizer());
                    return courseUnitEntity.isEmpty();
                }).collect(Collectors.toList());

                realisation.setUpdateTime(originalUpdateTime);
                realisation.setStudyElementReferences(validReferences);

                realisationRepository.update(realisation);
            } catch(Exception e) {
                logger.error("Error while rolling back; attempted to revert references to {} with id {}", realisation.getClass().getSimpleName(), realisation.getId(), e);
            }
        }
    }
}
