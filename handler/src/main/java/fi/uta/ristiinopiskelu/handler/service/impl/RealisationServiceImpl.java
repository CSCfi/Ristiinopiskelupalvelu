package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.realisation.RealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.exception.*;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
import fi.uta.ristiinopiskelu.handler.utils.KeyHelper;
import fi.uta.ristiinopiskelu.persistence.querybuilder.RealisationQueryBuilder;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class RealisationServiceImpl extends AbstractCompositeIdentifiedService<RealisationWriteDTO, RealisationEntity, RealisationReadDTO> implements RealisationService {

    private static final Logger logger = LoggerFactory.getLogger(RealisationServiceImpl.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    public RealisationServiceImpl() {
        super(RealisationWriteDTO.class, RealisationEntity.class, RealisationReadDTO.class);
    }

    @Override
    protected ExtendedRepository<RealisationEntity, String> getRepository() {
        return realisationRepository;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public List<CompositeIdentifiedEntityModificationResult> create(RealisationEntity realisationEntity) throws CreateFailedException {
        Assert.notNull(realisationEntity, "Realisation cannot be null");

        if(isValidateId()) {
            Assert.isNull(realisationEntity.getId(), "Id should be null but was " + realisationEntity.getId());
        }

        // Setup entity data
        realisationEntity.setStatus(realisationEntity.getStatus() != null ? realisationEntity.getStatus() : StudyStatus.ACTIVE);
        realisationEntity = super.setOrganizingOrganisationId(realisationEntity);
        realisationEntity = super.setCooperationNetworkData(realisationEntity);
        realisationEntity = super.fillMissingOrganisationInfo(realisationEntity);
        realisationEntity = super.setUniqueId(realisationEntity);

        try {
            RealisationEntity createdEntity = realisationRepository.create(realisationEntity, IndexQuery.OpType.CREATE);
            addOrUpdateDenormalizedData(createdEntity, createdEntity.getStudyElementReferences());
            return List.of(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.CREATE, CompositeIdentifiedEntityType.REALISATION, null, createdEntity));
        } catch(Exception e) {
            throw new CreateFailedException(getEntityClass(), e);
        }
    }

    @Override
    public List<CompositeIdentifiedEntityModificationResult> createAll(List<RealisationEntity> realisationEntities) throws CreateFailedException {
        Assert.notEmpty(realisationEntities, "Realisation entities cannot be null or empty");
        List<CompositeIdentifiedEntityModificationResult> createdRealisationEntities = new ArrayList<>();
        try {
            for(RealisationEntity realisation : realisationEntities) {
                createdRealisationEntities.addAll(this.create(realisation));
            }
        } catch (Exception e) {
            rollback(createdRealisationEntities);
            throw new CreateFailedException("Failed to create realisations.");
        }

        return createdRealisationEntities;
    }

    private void addOrUpdateDenormalizedData(RealisationEntity realisationToDenormalize, List<StudyElementReference> references) {
        if(CollectionUtils.isEmpty(references)) {
            return;
        }

        for(StudyElementReference reference : references) {
            CourseUnitEntity referencedCourseUnit = getReferencedCourseUnitEntity(reference);

            // This should be validated before calling and should not happen. Cannot throw exception here since it would
            // cause returning failure message for message sending organisation even though realisations have already been saved
            if(referencedCourseUnit == null) {
                logger.error("RealisationService:addDenormalizedData called with realisation that has reference to course unit that does not exist!" +
                    " this should never happen. Realisation references must be validated before calling updateDenormalizedData.");
                continue;
            }

            CourseUnitRealisationEntity courseUnitRealisationEntity = modelMapper.map(realisationToDenormalize, CourseUnitRealisationEntity.class);
            courseUnitRepository.saveHistory(referencedCourseUnit, CourseUnitEntity.class);
            addOrUpdateCourseUnitRealisation(reference, referencedCourseUnit, courseUnitRealisationEntity);
            courseUnitService.update(referencedCourseUnit);
        }
    }

    private void addOrUpdateCourseUnitRealisation(StudyElementReference reference, CourseUnitEntity referencedCourseUnit,
                                                  CourseUnitRealisationEntity newCourseUnitRealisationEntity) {
        Predicate<CourseUnitRealisationEntity> referenceMatchesToDenormalizedData =
            cur -> cur.getRealisationId().equals(newCourseUnitRealisationEntity.getRealisationId())
                && cur.getOrganizingOrganisationId().equals(newCourseUnitRealisationEntity.getOrganizingOrganisationId());

        if(reference.getReferenceType() == StudyElementType.ASSESSMENT_ITEM) {
            for(AssessmentItemEntity assessmentItemEntity : referencedCourseUnit.getAssessmentItems()) {
                if(assessmentItemEntity.getAssessmentItemId().equals(reference.getReferenceAssessmentItemId())) {
                    if(assessmentItemEntity.getRealisations() == null) {
                        assessmentItemEntity.setRealisations(new ArrayList<>());
                    }
                    assessmentItemEntity.getRealisations().removeIf(referenceMatchesToDenormalizedData);
                    assessmentItemEntity.getRealisations().add(newCourseUnitRealisationEntity);
                }
            }
        } else {
            if(referencedCourseUnit.getRealisations() == null) {
                referencedCourseUnit.setRealisations(new ArrayList<>());
            }
            referencedCourseUnit.getRealisations().removeIf(referenceMatchesToDenormalizedData);
            referencedCourseUnit.getRealisations().add(newCourseUnitRealisationEntity);
        }
    }

    private CourseUnitEntity getReferencedCourseUnitEntity(StudyElementReference reference) {
        if(reference.getReferenceType() == StudyElementType.ASSESSMENT_ITEM) {
            return courseUnitService.findByIdAndAssessmentItemIdAndOrganizer(
                reference.getReferenceIdentifier(), reference.getReferenceAssessmentItemId(), reference.getReferenceOrganizer())
                .orElse(null);
        } else {
            return courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                reference.getReferenceIdentifier(), reference.getReferenceOrganizer())
                .orElse(null);
        }
    }

    private void rollback(List<CompositeIdentifiedEntityModificationResult> createdRealisations) {
        List<KeyHelper> rollbackFailedEntities = new ArrayList<>();
        logger.info("Rollbacking CREATE_REALISATION_REQUEST.");
        for(CompositeIdentifiedEntityModificationResult realisation : createdRealisations) {
            try {
                realisationRepository.delete((RealisationEntity) realisation.getCurrent());
            } catch(Exception e) {
                rollbackFailedEntities.add(new KeyHelper(realisation.getCurrent().getElementId(), realisation.getCurrent().getOrganizingOrganisationId()));
            }
        }

        if(!CollectionUtils.isEmpty(rollbackFailedEntities)) {
            logger.error("Realisation rollback failed for realisations: [" +
                    String.join(", ", rollbackFailedEntities.stream().map(Object::toString).collect(Collectors.toList())) + "]");
        }
    }
    
    @Override
    public List<CompositeIdentifiedEntityModificationResult> update(JsonNode updateJson, String organisationId) throws UpdateFailedException {
        Assert.notNull(updateJson, "Realisation jsonNode cannot be null");
        Assert.notNull(updateJson.get("realisationId"), "Realisation json must have realisationId field");

        String realisationId = updateJson.get("realisationId").asText();
        String organizingOrganisationId = updateJson.has("organizingOrganisationId") ? updateJson.get("organizingOrganisationId").asText(null) : null;

        try {
            RealisationEntity originalForUpdating = realisationRepository
                .findByRealisationIdAndOrganizingOrganisationId(realisationId, organisationId)
                .orElseThrow(() -> new EntityNotFoundException(RealisationEntity.class, realisationId, organisationId));

            List<StudyElementReference> originalStudyElementReferences = originalForUpdating.getStudyElementReferences();
            StudyStatus originalStudyStatus = originalForUpdating.getStatus();

            realisationRepository.saveHistory(originalForUpdating, RealisationEntity.class);

            ObjectReader reader = objectMapper.readerForUpdating(originalForUpdating);
            RealisationEntity updatedRealisationEntity = reader.readValue(updateJson);

            // reset organizing organisation id if it was updated
            if(StringUtils.hasText(organizingOrganisationId) && !organizingOrganisationId.equals(organisationId)) {
                updatedRealisationEntity.setOrganizingOrganisationId(organisationId);
            }

            if(updatedRealisationEntity.getStatus() == null) {
                updatedRealisationEntity.setStatus(originalStudyStatus);
            }

            super.setCooperationNetworkData(updatedRealisationEntity);
            super.fillMissingOrganisationInfo(updatedRealisationEntity);

            handleDenormalizedData(realisationId, organisationId, originalStudyElementReferences,
                updateJson, updatedRealisationEntity);

            return List.of(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.UPDATE,
                CompositeIdentifiedEntityType.REALISATION, originalForUpdating, realisationRepository.update(updatedRealisationEntity)));
        } catch (Exception e) {
            throw new UpdateFailedException(getEntityClass(), realisationId, e);
        }
    }

    @Override
    public List<CompositeIdentifiedEntityModificationResult> delete(String realisationId, String organizingOrganisationId) throws DeleteFailedException {
        Assert.hasText(realisationId, "Realisation id cannot be empty");
        Assert.hasText(organizingOrganisationId, "Realisation organizingOrganisationId cannot be empty");

        try {
            RealisationEntity realisationEntity = realisationRepository.findByRealisationIdAndOrganizingOrganisationId(
                realisationId, organizingOrganisationId).orElseThrow(() -> new EntityNotFoundException(RealisationEntity.class, realisationId));
            
            removeDenormalizedData(realisationEntity.getRealisationId(), realisationEntity.getOrganizingOrganisationId(), realisationEntity.getStudyElementReferences());
            realisationRepository.saveHistory(realisationEntity, RealisationEntity.class);
            realisationRepository.delete(realisationEntity);

            return List.of(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.DELETE, CompositeIdentifiedEntityType.REALISATION, realisationEntity, null));
        } catch (Exception e) {
            throw new DeleteFailedException(getEntityClass(), realisationId, organizingOrganisationId, e);
        }
    }

    private void handleDenormalizedData(String realisationId, String organizingOrganisationId,
                                        List<StudyElementReference> originalStudyElementReferences, JsonNode updateJson,
                                        RealisationEntity updated) {
        List<StudyElementReference> existingReferences;

        if(updateJson.has("studyElementReferences")) {
            // This should not happen, if studyElementReferences are given with update message, it should always be validated
            // that studyElementReferences list is not empty
            if(updated.getStudyElementReferences() == null) {
                logger.error("Trying to handle denormalized data for changed studyElementReferences but received UPDATE_REALISATION_REQUEST-message" +
                    " that has 'studyElementReferences=null'. This should not happen since Realisation cannot exist without any references." +
                    " StudyElementReferences should be validated before entering service layer update, is there a bug?");
                return;
            }

            List<StudyElementReference> removedReferences = new ArrayList<>();
            if(!CollectionUtils.isEmpty(originalStudyElementReferences)) {
                removedReferences = originalStudyElementReferences.stream()
                    .filter(se -> !updated.getStudyElementReferences().contains(se)).collect(Collectors.toList());
            }

            List<StudyElementReference> newReferences = updated.getStudyElementReferences().stream()
                .filter(se -> !CollectionUtils.isEmpty(originalStudyElementReferences)
                    && !originalStudyElementReferences.contains(se))
                .collect(Collectors.toList());

            addOrUpdateDenormalizedData(updated, newReferences);
            removeDenormalizedData(realisationId, organizingOrganisationId, removedReferences);

            existingReferences = updated.getStudyElementReferences().stream()
                .filter(ser -> !newReferences.contains(ser))
                .collect(Collectors.toList());

        } else {
            existingReferences = originalStudyElementReferences;
        }

        addOrUpdateDenormalizedData(updated, existingReferences);
    }

    private void removeDenormalizedData(String realisationId, String realisationOrganizingOrganisationId, List<StudyElementReference> references) {
        if(CollectionUtils.isEmpty(references)) {
            return;
        }

        for(StudyElementReference reference : references) {
            CourseUnitEntity referencedCourseUnit = getReferencedCourseUnitEntity(reference);

            // This should be validated before calling and should not happen. Cannot throw exception here since it would
            // cause returning failure message for message sending organisation even though realisations have already been saved
            if(referencedCourseUnit == null) {
                logger.error("RealisationService:removeDenormalizedData called with realisation that has reference to course unit that does not exist!" +
                    " this should never happen. Realisation references must be validated before calling updateDenormalizedData.");
                continue;
            }

            courseUnitRepository.saveHistory(referencedCourseUnit, CourseUnitEntity.class);

            removeCourseUnitRealisation(realisationId, realisationOrganizingOrganisationId, reference, referencedCourseUnit);
            courseUnitService.update(referencedCourseUnit);
        }

    }

    private void removeCourseUnitRealisation(String realisationId, String realisationOrganizingOrganisationId, StudyElementReference reference, CourseUnitEntity referencedCourseUnit) {
        Predicate<CourseUnitRealisationEntity> referenceMatchesToDenormalizedData =
            cur -> cur.getRealisationId().equals(realisationId)
                && cur.getOrganizingOrganisationId().equals(realisationOrganizingOrganisationId);

        if(reference.getReferenceType() == StudyElementType.ASSESSMENT_ITEM) {
            for(AssessmentItemEntity assessmentItemEntity : referencedCourseUnit.getAssessmentItems()) {
                if(assessmentItemEntity.getAssessmentItemId().equals(reference.getReferenceAssessmentItemId())) {
                    if(!CollectionUtils.isEmpty(assessmentItemEntity.getRealisations())) {
                        assessmentItemEntity.getRealisations().removeIf(referenceMatchesToDenormalizedData);
                    }

                    if(CollectionUtils.isEmpty(assessmentItemEntity.getRealisations())) {
                        assessmentItemEntity.setRealisations(null);
                    }
                }
            }
        } else {
            if(!CollectionUtils.isEmpty(referencedCourseUnit.getRealisations())) {
                referencedCourseUnit.getRealisations().removeIf(referenceMatchesToDenormalizedData);
                if(CollectionUtils.isEmpty(referencedCourseUnit.getRealisations())) {
                    referencedCourseUnit.setRealisations(null);
                }
            }
        }
    }

    @Override
    public Optional<RealisationEntity> findByIdAndOrganizingOrganisationId(String id, String organisationId) throws FindFailedException {
        Assert.hasText(id, "Realisation id cannot be empty");
        Assert.hasText(organisationId, "Realisation organizingOrganisationId cannot be empty");

        try {
            return realisationRepository.findByRealisationIdAndOrganizingOrganisationId(id, organisationId);
        } catch(Exception e) {
            throw new FindFailedException(getEntityClass(), id, organisationId, e);
        }
    }

    @Override
    public List<RealisationEntity> findByStudyElementReference(String referenceIdentifier, String referenceOrganizer) throws FindFailedException {
        try {
            return realisationRepository.findByStudyElementReference(referenceIdentifier, referenceOrganizer, RealisationEntity.class);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), referenceIdentifier, referenceOrganizer, e);
        }
    }

    @Override
    public List<RealisationEntity> findByAssessmentItemReference(String referenceIdentifier, String referenceOrganizer, String assessmentItemId) {
        try {
            return realisationRepository.findByAssessmentItemReference(referenceIdentifier, referenceOrganizer, assessmentItemId);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), referenceIdentifier, referenceOrganizer, e);
        }
    }

    @Override
    public RealisationSearchResults searchByIds(String organisationId, String realisationId, String organizingOrganisationId,
                                                List<StudyStatus> statuses, Pageable pageable) {

        if(StringUtils.hasText(realisationId) && !StringUtils.hasText(organizingOrganisationId)) {
            throw new InvalidSearchParametersException("organizingOrganisationId must be specified if realisationId is specified");
        }

        RealisationQueryBuilder realisationQuery = new RealisationQueryBuilder();
        
        List<NetworkEntity> networksToSearch = networkService.findAllValidNetworksWhereOrganisationIsValid(organisationId, Pageable.unpaged());

        realisationQuery.filterByCooperationNetworks(organisationId, networksToSearch, null, true, true);

        realisationQuery.filterByComposedId(realisationId, null, organizingOrganisationId);

        if(!CollectionUtils.isEmpty(statuses)) {
            realisationQuery.filterByStatuses(statuses);
        }

        List<RealisationReadDTO> results = getRepository().search(realisationQuery.build()._toQuery(), pageable).stream()
            .map(this::toReadDTO)
            .collect(Collectors.toList());

        return new RealisationSearchResults(results);
    }

    @Override
    public RealisationSearchResults search(String organisationId, RealisationSearchParameters searchParams) throws FindFailedException {
        List<NetworkEntity> organisationNetworks;

        if(!searchParams.isIncludeInactive()) {
            organisationNetworks = networkService.findAllValidNetworksWhereOrganisationIsValid(organisationId, Pageable.unpaged());
        } else {
            organisationNetworks = networkService.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged());
        }

        RealisationQueryBuilder realisationQuery = new RealisationQueryBuilder();

        // Filter to return only study elements for requesting organisations networks
        realisationQuery.filterByCooperationNetworks(organisationId, organisationNetworks, searchParams.getNetworkIdentifiers(),
            searchParams.isIncludeInactive(), searchParams.isIncludeOwn());

        // Filter by given realisation id and identifier code
        realisationQuery.filterByComposedId(searchParams.getRealisationId(), searchParams.getRealisationIdentifierCode(),
            searchParams.getOrganizingOrganisationId());

        // Find by name
        if(StringUtils.hasText(searchParams.getQuery())) {
            realisationQuery.filterByName(searchParams.getQuery(), searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI);
        }

        // Find only realisations that have ongoing enrollment
        if(searchParams.isOngoingEnrollment()) {
            realisationQuery.filterOngoingEnrollments();
        }

        // Filter past realisations
        if(!searchParams.isIncludePast()) {
            realisationQuery.filterPastEndDate();
        }

        // Find only realisations that have reference to given course unit
        if(!CollectionUtils.isEmpty(searchParams.getCourseUnitReferences())) {
            realisationQuery.filterByCourseUnitReferences(searchParams.getCourseUnitReferences());
        }

        if(!CollectionUtils.isEmpty(searchParams.getStatuses())) {
            realisationQuery.filterByStatuses(searchParams.getStatuses());
        }

        if(!CollectionUtils.isEmpty(searchParams.getMinEduGuidanceAreas())) {
            realisationQuery.filterByMinEduGuidanceAreas(searchParams.getMinEduGuidanceAreas());
        }

        List<RealisationReadDTO> results = realisationRepository.search(realisationQuery.build()._toQuery(),
                searchParams.getPageRequest()).stream()
            .map(this::toReadDTO)
            .collect(Collectors.toList());

        return new RealisationSearchResults(results);
    }
}
