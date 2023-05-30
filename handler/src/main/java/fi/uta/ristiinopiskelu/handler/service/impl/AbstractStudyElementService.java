package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.AssessmentItemReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CompletionOptionReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitRealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.degree.DegreeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.DefaultStudyElementSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.StudyElementSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.StudyElementSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.degree.DegreeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.exception.*;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.StudyElementEntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.StudiesService;
import fi.uta.ristiinopiskelu.handler.service.StudyElementService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.DefaultCompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.persistence.querybuilder.StudiesQueryBuilder;
import fi.uta.ristiinopiskelu.persistence.repository.StudyElementRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractStudyElementService<D extends AbstractStudyElementWriteDTO, T extends StudyElementEntity, R extends AbstractStudyElementReadDTO>
    extends AbstractCompositeIdentifiedService<D, T, R> implements StudyElementService<D, T, R>, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(AbstractStudyElementService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private StudiesService studiesService;

    private Repositories repositories;
    private StudyElementType studyElementType;

    // Empty constructor for mocking in tests do not use outside of tests
    public AbstractStudyElementService() {}

    public AbstractStudyElementService(Class<D> dtoClazz, Class<T> entityClazz, Class<R> restDtoClazz, StudyElementType studyElementType) {
        super(dtoClazz, entityClazz, restDtoClazz);
        this.studyElementType = studyElementType;
    }

    public StudyElementType getStudyElementType() {
        return studyElementType;
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected abstract StudyElementRepository<T> getRepository();

    protected <Y extends StudyElementEntity> StudyElementRepository<Y> getRepositoryForStudyElementWriteDTO(AbstractStudyElementWriteDTO dto) {
        if(dto == null) {
            throw new IllegalArgumentException("AbstractStudyElementWriteDTO cannot be null");
        }

        StudyElementType type = dto.getType();

        if(type == null) {
            throw new IllegalStateException("AbstractStudyElementWriteDTO type should not be null");
        }

        return getRepositoryForEntityClass(type.getStudyElementEntityClass());
    }

    protected <Y extends StudyElementEntity> StudyElementRepository<Y> getRepositoryForEntityClass(Class<? extends StudyElementEntity> entityClass) {
        Assert.notNull(entityClass, "Entity class cannot be null");

        return (StudyElementRepository<Y>) repositories.getRepositoryFor(entityClass)
            .orElseThrow(() -> new IllegalStateException("No repository found for entity class " + entityClass.getSimpleName()));
    }

    @Override
    public T create(T entity) throws CreateFailedException {
        Assert.notNull(entity, "Entity cannot be null");

        if(isValidateId()) {
            Assert.isNull(entity.getId(), "Id should be null but was " + entity.getId());
        }

        entity.setStatus(entity.getStatus() != null ? entity.getStatus() : StudyStatus.ACTIVE);
        entity = super.setOrganizingOrganisationId(entity);
        entity = super.setCooperationNetworkData(entity);
        entity = super.fillMissingOrganisationInfo(entity);
        entity = super.setUniqueId(entity);

        try {
            return (T) this.getRepositoryForEntityClass(entity.getClass()).create(entity, IndexQuery.OpType.CREATE);
        } catch(Exception e) {
            throw new CreateFailedException(getEntityClass(), e);
        }
    }

    @Override
    public List<T> createAll(List<T> entities) throws CreateFailedException {
        Assert.notEmpty(entities, "Entities cannot be null or empty");

        List<T> createdEntities = new ArrayList<>();

        for (T entity : entities) {
            try {
                createdEntities.add(this.create(entity));
            } catch (Exception e) {
                logger.error("Error while persisting entity {}, attempting to delete {} already persisted from index", entity, createdEntities.size(), e);

                for (T created : createdEntities) {
                    try {
                        getRepository().deleteById(created.getId());
                        logger.info("Successfully deleted entity with id {}", created.getId());
                    } catch (Exception e1) {
                        logger.error("Error while deleting already persisted entity {}", created.getId(), e1);
                    }
                }

                throw new CreateFailedException(getEntityClass(), e);
            }
        }

        return createdEntities;
    }

    @Override
    public T update(JsonNode json, String organisationId) throws UpdateFailedException {
        Assert.notNull(json, "Json cannot be null");
        Assert.notNull(json.get("studyElementId"), "Json must have studyElementId field");
        Assert.hasText(organisationId, "Missing organisation JMS header");

        String id = json.get("studyElementId").asText();
        String organizingOrganisationId = json.has("organizingOrganisationId") ? json.get("organizingOrganisationId").asText(null) : null;


        // Since entity does not have field subElements, remove field here in case its given. SubElements should be handled before coming here (eg. in child service)
        if(json.has("subElements")) {
            ((ObjectNode)json).remove("subElements");
        }

        try {
            T original = this.getRepository().findByStudyElementIdAndOrganizingOrganisationId(id, organisationId)
                    .orElseThrow(() -> new StudyElementEntityNotFoundException(getEntityClass(), id, organisationId));

            this.getRepository().saveHistory(original, this.getEntityClass());

            ObjectReader reader = objectMapper.readerForUpdating(original);
            T updatedEntity = reader.readValue(json);

            // reset organizing organisation id if it was updated
            if(!StringUtils.isEmpty(organizingOrganisationId) && !organizingOrganisationId.equals(original.getOrganizingOrganisationId())) {
                updatedEntity.setOrganizingOrganisationId(original.getOrganizingOrganisationId());
            }

            if(updatedEntity.getStatus() == null) {
                updatedEntity.setStatus(original.getStatus());
            }

            super.setCooperationNetworkData(updatedEntity);
            super.fillMissingOrganisationInfo(updatedEntity);
            return this.getRepository().update(updatedEntity);
        } catch (Exception e) {
            throw new UpdateFailedException(getEntityClass(), id, e);
        }
    }

    @Override
    public T deleteByStudyElementIdAndOrganizingOrganisationId(String studyElementId, String organizingOrganisationId) throws DeleteFailedException {
        Assert.hasText(studyElementId, "studyElementId cannot be empty");
        Assert.hasText(organizingOrganisationId, "Missing organisation JMS header");

        try {
            T original = this.getRepository().findByStudyElementIdAndOrganizingOrganisationId(studyElementId, organizingOrganisationId)
                .orElseThrow(() -> new EntityNotFoundException(this.getEntityClass(), studyElementId));
            getRepository().deleteById(original.getId());
            return original;
        } catch(Exception e) {
            throw new DeleteFailedException(getEntityClass(), e);
        }
    }

    @Override
    public Optional<T> findByIdAndCooperationNetworkIds(String id, List<String> cooperationNetworkIds) throws FindFailedException {
        Assert.hasText(id, "Id cannot be empty");
        Assert.notEmpty(cooperationNetworkIds, "Cooperation network ids cannot be empty");
        try {
            List<T> result = this.getRepository().findByIdInAndCooperationNetworksIdIn(Collections.singletonList(id),
                    cooperationNetworkIds).orElse(Collections.emptyList());

            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), id, e);
        }
    }

    @Override
    public List<T> findByIdsAndCooperationNetworkIds(List<String> ids, List<String> cooperationNetworkIds) throws FindFailedException {
        Assert.notEmpty(ids, "Ids cannot be empty");
        Assert.notEmpty(cooperationNetworkIds, "Cooperation network ids cannot be empty");
        try {
            return this.getRepository().findByIdInAndCooperationNetworksIdIn(ids, cooperationNetworkIds).orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), ids, e);
        }
    }

    @Override
    public Optional<T> findByIdAndOrganizingOrganisationIds(String id, List<String> organizingOrganisationIds) throws FindFailedException {
        Assert.hasText(id, "Id cannot be empty");
        Assert.notEmpty(organizingOrganisationIds, "Organisation ids cannot be empty");

        try {
            List<T> result = this.getRepository().findByIdInAndOrganizingOrganisationIdIn(Collections.singletonList(id),
                    organizingOrganisationIds).orElse(Collections.emptyList());

            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), id, e);
        }
    }

    @Override
    public List<T> findByIdsAndOrganizingOrganisationIds(List<String> ids, List<String> organizingOrganisationIds) throws FindFailedException {
        Assert.notEmpty(ids, "Ids cannot be empty");
        Assert.notEmpty(organizingOrganisationIds, "Organisation id cannot be empty");
        try {
            return this.getRepository().findByIdInAndOrganizingOrganisationIdIn(ids, organizingOrganisationIds).orElse(Collections.emptyList());
        } catch(Exception e) {
            throw new FindFailedException(getEntityClass(), ids, e);
        }
    }

    @Override
    public List<T> findAllByOrganizingOrganisationIds(List<String> organizingOrganisationIds) throws FindFailedException {
        try {
            return this.getRepository().findByOrganizingOrganisationIdIn(organizingOrganisationIds).orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public List<T> findAllByCooperationNetworkIds(List<String> cooperationNetworkIds) throws FindFailedException {
        try {
            return this.getRepository().findByCooperationNetworksIdIn(cooperationNetworkIds).orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public Optional<T> findByStudyElementIdAndOrganizingOrganisationId(String studyElementId, String organizingOrganisationId) throws FindFailedException {
        try {
            return this.getRepository().findByStudyElementIdAndOrganizingOrganisationId(studyElementId,
                    organizingOrganisationId);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), studyElementId, organizingOrganisationId, e);
        }
    }

    @Override
    public List<T> findByStudyElementReference(String referenceIdentifier, String referenceOrganizer) throws FindFailedException {
        try {
            return this.getRepository().findByStudyElementReference(referenceIdentifier, referenceOrganizer, this.getEntityClass());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), referenceIdentifier, referenceOrganizer, e);
        }
    }

    /**
     * Persists all given studyElements to ES with complete subElement hierarchy, if included.
     * @param studyElements
     * @param organisationId
     * @return
     */
    @Override
    public CompositeIdentifiedEntityModificationResult createAll(List<D> studyElements, String organisationId) throws CreateFailedException {
        List<StudyElementEntity> created = new ArrayList<>();
        HashMap<OffsetDateTime, StudyElementEntity> updated = new HashMap<>();

        try {
            for (D studyElement : studyElements) {
                StudyElementEntity existing = getRepository().findByStudyElementIdAndOrganizingOrganisationId(
                        studyElement.getStudyElementId(), organisationId).orElse(null);

                if(existing != null) {
                    throw new IllegalStateException(existing.getClass().getSimpleName() + " already exists with id " + existing.getId());
                }

                if(!CollectionUtils.isEmpty(studyElement.getSubElements())) {
                    logger.debug("Create or update {} subElements", studyElement.getSubElements().size());
                    createOrUpdateSubElements(studyElement, studyElement.getSubElements(), organisationId, created, updated);
                }

                T entity = modelMapper.map(studyElement, getEntityClass());

                created.add(this.create(entity));
                logger.debug("Created {} with id {}", entity.getClass().getSimpleName(), entity.getId());
            }
        } catch(Exception e) {
            rollback(created, updated);
            throw new CreateFailedException(getEntityClass(), e);
        }

        return new DefaultCompositeIdentifiedEntityModificationResult(created, new ArrayList<>(updated.values()));
    }

    @Override
    public StudyElementSearchResults<R> search(String organisationId, StudyElementSearchParameters<T> searchParameters) throws FindFailedException, InvalidSearchParametersException {
        List<NetworkEntity> organisationNetworks;

        if(!searchParameters.isIncludeInactive()) {
            organisationNetworks = networkService.findAllValidNetworksWhereOrganisationIsValid(organisationId, Pageable.unpaged());
        } else {
            organisationNetworks = networkService.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged());
        }

        StudiesQueryBuilder studiesQueryBuilder = new StudiesQueryBuilder();

        if(!CollectionUtils.isEmpty(searchParameters.getStatuses())) {
            studiesQueryBuilder.filterByStatuses(searchParameters.getStatuses());
        }

        studiesQueryBuilder.filterByComposedId(searchParameters.getStudyElementId(), searchParameters.getStudyElementIdentifierCode(),
            searchParameters.getOrganizingOrganisationId());

        // filter out inactive study elements
        if(!searchParameters.isIncludeInactive()) {
            studiesQueryBuilder.filterOnlyValid();
        }

        // must be in our valid networks
        studiesQueryBuilder.filterByCooperationNetworks(organisationId, organisationNetworks, null,
            searchParameters.isIncludeInactive(), true);

        List<R> results = getRepository().search(studiesQueryBuilder, searchParameters.getPageRequest())
            .stream()
            .map(this::toReadDTO)
            .collect(Collectors.toList());

        populateSubElementHierarchy(results, new RealisationHasValidNetworkFilter(organisationId, organisationNetworks,
            searchParameters.isIncludeInactive()));

        try {
            return new DefaultStudyElementSearchResults<>(results);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.repositories = new Repositories(applicationContext);
    }

    private void populateSubElementHierarchy(List<? extends AbstractStudyElementReadDTO> elements,  Predicate<CourseUnitRealisationReadDTO> realisationFilter) {
        if(CollectionUtils.isEmpty(elements)) {
            return;
        }

        for(AbstractStudyElementReadDTO studyElement : elements) {
            if(studyElement instanceof CourseUnitReadDTO) {
                CourseUnitReadDTO courseUnit = (CourseUnitReadDTO) studyElement;

                if(!CollectionUtils.isEmpty(courseUnit.getRealisations())) {
                    courseUnit.getRealisations().removeIf(Predicate.not(realisationFilter));
                }
                if(!CollectionUtils.isEmpty(courseUnit.getCompletionOptions())) {
                    for(CompletionOptionReadDTO completionOption : courseUnit.getCompletionOptions()) {
                        if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                            for (AssessmentItemReadDTO assessmentItem : completionOption.getAssessmentItems()) {
                                if (!CollectionUtils.isEmpty(assessmentItem.getRealisations())) {
                                    assessmentItem.getRealisations().removeIf(Predicate.not(realisationFilter));
                                }
                            }
                        }
                    }
                }
            }

            List<AbstractStudyElementReadDTO> subElements = studiesService.findAllStudiesByParentReferences(studyElement.getStudyElementId(),
                    super.getOrganizingOrganisationIdFromReferences(studyElement.getOrganisationReferences()))
                .stream().map(se -> {
                    if(se instanceof CourseUnitEntity) {
                        return modelMapper.map(se, CourseUnitReadDTO.class);
                    } else if(se instanceof StudyModuleEntity) {
                        return modelMapper.map(se, StudyModuleReadDTO.class);
                    } else if(se instanceof DegreeEntity) {
                        return modelMapper.map(se, DegreeReadDTO.class);
                    } else {
                        throw new IllegalStateException("Unknown StudyElement type: " + se.getClass().getSimpleName());
                    }
                }).collect(Collectors.toList());

            populateSubElementHierarchy(subElements, realisationFilter);
            studyElement.setSubElements(subElements);
        }
    }

    private static class RealisationHasValidNetworkFilter implements Predicate<CourseUnitRealisationReadDTO> {

        private final String organisationId;
        private final List<NetworkEntity> validNetworks = new ArrayList<>();
        private final boolean includeInactive;

        public RealisationHasValidNetworkFilter(String organisationId, List<NetworkEntity> validNetworks, boolean includeInactive) {
            this.organisationId = organisationId;
            this.includeInactive = includeInactive;

            if(!CollectionUtils.isEmpty(validNetworks)) {
                this.validNetworks.addAll(validNetworks);
            }
        }

        @Override
        public boolean test(CourseUnitRealisationReadDTO courseUnitRealisationEntity) {
            boolean isOwn = courseUnitRealisationEntity.getOrganizingOrganisationId().equals(organisationId);

            boolean hasValidNetworks = !CollectionUtils.isEmpty(courseUnitRealisationEntity.getCooperationNetworks()) &&
                courseUnitRealisationEntity.getCooperationNetworks().stream().anyMatch(cn -> {
                    LocalDate now = LocalDate.now();

                    boolean hasValidDates = (cn.getValidityStartDate() == null && cn.getValidityEndDate() == null) ||
                    ((cn.getValidityStartDate() == null || !cn.getValidityStartDate().isAfter(now)) && (cn.getValidityEndDate() == null || !cn.getValidityEndDate().isBefore(LocalDate.now())));

                    boolean isVisibleToOrganisation = validNetworks.stream().anyMatch(network -> network.getId().equals(cn.getId()) &&
                        (network.isPublished() || (!network.isPublished() && isOwn)));

                    return includeInactive ? isVisibleToOrganisation : hasValidDates && isVisibleToOrganisation;
            });

            return includeInactive ? (isOwn || hasValidNetworks) : hasValidNetworks;
        }
    }

    /**
     * Persists all given subElements and their complete hierarchy to ES recursively.
     * @param subElements
     * @param parent
     * @param organisationId
     * @param created
     * @param updated
     */
    private void createOrUpdateSubElements(AbstractStudyElementWriteDTO parent, List<AbstractStudyElementWriteDTO> subElements, String organisationId,
                                           List<StudyElementEntity> created, HashMap<OffsetDateTime, StudyElementEntity> updated) {

        if (!CollectionUtils.isEmpty(subElements)) {
            for (AbstractStudyElementWriteDTO subElement : subElements) {
                StudyElementReference parentReference = getParentReference(parent.getStudyElementId(), organisationId, parent.getType(), subElement);

                StudyElementEntity existing = getRepositoryForStudyElementWriteDTO(subElement).findByStudyElementIdAndOrganizingOrganisationId(
                        subElement.getStudyElementId(), organisationId).orElse(null);

                // check if reference is okay on the realisation, if not, add it.
                if (existing != null) {
                    getRepositoryForStudyElementWriteDTO(subElement).saveHistory(existing, existing.getClass());
                    existing = checkReferences(existing, parentReference);
                    updated.put(existing.getUpdateTime(), getRepositoryForStudyElementWriteDTO(subElement).update(existing));
                    logger.debug("Updated existing subElement {} with id {}", existing.getClass().getSimpleName(), existing.getId());
                } else {
                    StudyElementEntity entity;

                    if(subElement instanceof CourseUnitWriteDTO) {
                        entity = modelMapper.map(subElement, CourseUnitEntity.class);
                    } else if(subElement instanceof StudyModuleWriteDTO) {
                        entity = modelMapper.map(subElement, StudyModuleEntity.class);
                    } else if(subElement instanceof DegreeWriteDTO) {
                        entity = modelMapper.map(subElement, DegreeEntity.class);
                    } else {
                        throw new IllegalStateException("Unidentified subElement type: " + subElement.getClass().getSimpleName());
                    }

                    entity = checkReferences(entity, parentReference);

                    created.add(this.create((T)entity));
                    logger.debug("Created subElement {} with id {}", entity.getClass().getSimpleName(), entity.getId());
                }

                this.createOrUpdateSubElements(subElement, subElement.getSubElements(), organisationId, created, updated);
            }
        }
    }

    protected StudyElementReference getParentReference(String parentId, String parentOrganisationId, StudyElementType parentType,
                                                       AbstractStudyElementWriteDTO subElement) {
        Boolean onlyEnrollableWithParent = null;
        if(subElement instanceof CourseUnitWriteDTO) {
            onlyEnrollableWithParent = ((CourseUnitWriteDTO) subElement).getOnlyEnrollableWithParent();
        }
        return new StudyElementReference(parentId, parentOrganisationId, parentType, onlyEnrollableWithParent);
    }

    private StudyElementEntity checkReferences(StudyElementEntity entity, StudyElementReference reference) {
        if (!CollectionUtils.isEmpty(entity.getParents())) {
            if (!entity.getParents().contains(reference)) {
                entity.getParents().add(reference);
            }
        } else {
            entity.setParents(Collections.singletonList(reference));
        }

        logger.debug("Added studyElementReference to {} with id {}: {}", entity.getClass().getSimpleName(), entity.getId(), reference.toString());

        return entity;
    }

    private void rollback(List<StudyElementEntity> created, HashMap<OffsetDateTime, StudyElementEntity>  updated) {
        for (StudyElementEntity entity : created) {
            logger.debug("Deleting already persisted {} with id {}", entity.getClass().getSimpleName(), entity.getId());
            try {
                getRepositoryForEntityClass(entity.getClass()).deleteById(entity.getId());
            } catch(Exception e) {
                logger.error("Error while rolling back; attempted to delete {} with id {}", entity.getClass().getSimpleName(), entity.getId(), e);
            }
        }

        for(Map.Entry<OffsetDateTime, StudyElementEntity> entry : updated.entrySet()) {
            OffsetDateTime originalUpdateTime = entry.getKey();
            StudyElementEntity entity = entry.getValue();
            logger.debug("Removing reference from updated {} with id {}", entity.getClass().getSimpleName(), entity.getId());

            if(CollectionUtils.isEmpty(entity.getParents())) {
                continue;
            }

            try {
                // note: this returns references other than the one that matches
                List<StudyElementReference> validReferences = entity.getParents().stream().filter(sre -> {
                    StudyElementEntity referenced = (StudyElementEntity) getRepositoryForEntityClass(entity.getClass()).findByStudyElementIdAndOrganizingOrganisationId(
                            sre.getReferenceIdentifier(), sre.getReferenceOrganizer()).orElse(null);
                    return referenced == null ? true : false;
                }).collect(Collectors.toList());

                entity.setUpdateTime(originalUpdateTime);
                entity.setParents(validReferences);

                logger.debug("Setting studyElement references to {} with id {}: {}", entity.getClass().getSimpleName(), entity.getId(), validReferences.toString());
                getRepositoryForEntityClass(entity.getClass()).update(entity);
            } catch(Exception e) {
                logger.error("Error while rolling back; attempted to revert references to {} with id {}", entity.getClass().getSimpleName(), entity.getId(), e);
            }
        }
    }
}
