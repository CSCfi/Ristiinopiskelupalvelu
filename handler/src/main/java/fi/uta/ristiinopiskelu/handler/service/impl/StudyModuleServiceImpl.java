package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.exception.DeleteFailedException;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.StudyElementEntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.StudyElementReferenceIdentifiersMissingValidationException;
import fi.uta.ristiinopiskelu.handler.exception.validation.StudyElementReferenceNotInSameNetworkValidationException;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.persistence.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class StudyModuleServiceImpl extends AbstractStudyElementService<StudyModuleWriteDTO, StudyModuleEntity, StudyModuleReadDTO> implements StudyModuleService {

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private ModelMapper modelMapper;

    public StudyModuleServiceImpl() {
        super(StudyModuleWriteDTO.class, StudyModuleEntity.class, StudyModuleReadDTO.class, StudyElementType.STUDY_MODULE);
    }

    @Override
    protected StudyElementRepository<StudyModuleEntity> getRepository() {
        return studyModuleRepository;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public StudyModuleEntity delete(String studyElementId, String organizingOrganisationId, boolean deleteCourseUnits) throws DeleteFailedException {
        List<CourseUnitEntity> courseUnits = courseUnitRepository.findByStudyElementReference(
                studyElementId, organizingOrganisationId, CourseUnitEntity.class);

        if(!CollectionUtils.isEmpty(courseUnits)) {
            if(deleteCourseUnits) {
                for(CourseUnitEntity courseUnit : courseUnits) {
                    Predicate<StudyElementReference> matches =
                            ser -> ser.getReferenceIdentifier().equals(studyElementId) &&
                                ser.getReferenceOrganizer().equals(organizingOrganisationId);

                    courseUnitRepository.saveHistory(courseUnit, CourseUnitEntity.class);
                    courseUnit.getParents().removeIf(matches);
                    courseUnitRepository.update(courseUnit);
                }
            } else {
                throw new DeleteFailedException("Study module cannot be deleted if it has course units and deleteCourseUnits is false.");
            }
        }

        try {

            StudyModuleEntity studyModuleEntity = studyModuleRepository.findByStudyElementIdAndOrganizingOrganisationId(
                    studyElementId, organizingOrganisationId).orElseThrow(() -> new EntityNotFoundException(StudyModuleEntity.class, studyElementId));

            studyModuleRepository.saveHistory(studyModuleEntity, StudyModuleEntity.class);
            studyModuleRepository.delete(studyModuleEntity);

            return studyModuleEntity;
        } catch(Exception e) {
            throw new DeleteFailedException(getEntityClass(), studyElementId, organizingOrganisationId, e);
        }
    }

    @Override
    public StudyModuleEntity update(JsonNode json, String organisationId) throws UpdateFailedException {
        Assert.notNull(json, "Json cannot be null");
        Assert.notNull(json.get("studyElementId"), "Json must have studyElementId field");
        Assert.hasText(organisationId, "Missing organisation JMS header");

        String id = json.get("studyElementId").asText();
        List<CooperationNetwork> studyModuleCooperationNetworks = new ArrayList<>();

        if(json.has("cooperationNetworks")) {
            studyModuleCooperationNetworks.addAll(getObjectMapper().convertValue(json.get("cooperationNetworks"), new TypeReference<List<CooperationNetwork>>() {}));
        }

        try {
            if(json.has("subElements")) {
                List<AbstractStudyElementWriteDTO> subElements = getObjectMapper().convertValue(json.get("subElements"), new TypeReference<>(){});
                updateStudyModuleReferences(id, organisationId, studyModuleCooperationNetworks, subElements);
            }
        } catch (Exception e) {
            throw new UpdateFailedException(getEntityClass(), id, e);
        }

        return super.update(json, organisationId);
    }

    // This method can throw exception if no studyElement is found with given ids so this must be called before saving anything to elasticsearch
    private void updateStudyModuleReferences(String parentStudyModuleId, String organisationId, List<CooperationNetwork> parentCooperationNetworks,
                                             List<AbstractStudyElementWriteDTO> subElements) throws Exception {
        if(subElements == null) {
            subElements = new ArrayList<>();
        }

        for(AbstractStudyElementWriteDTO subElement : subElements) {
            if(StringUtils.isEmpty(subElement.getStudyElementId())) {
                throw new StudyElementReferenceIdentifiersMissingValidationException("Unable to handle update study module. " +
                        "Message contains " + subElement.getType() + " sub element that has missing identifiers: [studyElementId=" +
                        subElement.getStudyElementId() + ", organizingOrganisation: " +
                        subElement.getOrganizingOrganisationId() + "]");
            }
            
            if(StringUtils.isEmpty(subElement.getOrganizingOrganisationId())) {
                throw new StudyElementReferenceIdentifiersMissingValidationException("Unable to handle update study module. " +
                        "Message contains " + subElement.getType() + " sub element that doesn't have an organisation reference with role " +
                        OrganisationRole.ROLE_MAIN_ORGANIZER + ", identifiers: [studyElementId=" + subElement.getStudyElementId() +
                        ", " + subElement.getOrganizingOrganisationId() + "]");
            }
        }

        List<StudyElementEntity> referencesStudyElements = new ArrayList<>();
        referencesStudyElements.addAll(courseUnitRepository.findByStudyElementReference(parentStudyModuleId, organisationId, CourseUnitEntity.class));
        referencesStudyElements.addAll(studyModuleRepository.findByStudyElementReference(parentStudyModuleId, organisationId, StudyModuleEntity.class));

        List<AbstractStudyElementWriteDTO> studyElementsWithAddedOrUpdatedReferences =
            getStudyElementsWithAddedOrUpdatedReferences(subElements, referencesStudyElements);

        // Key entity to add to, value is reference to add
        // This is done as complex as is because we need to get given onlyEnrollableWithParent if it was set to verify if reference has been updated
        HashMap<StudyElementEntity, StudyElementReference> refsToAddAndStudyElementEntities =
            getReferencesAndStudyElementsToAddRefTo(parentStudyModuleId, organisationId, parentCooperationNetworks,
                studyElementsWithAddedOrUpdatedReferences);

        upsertStudyElementReferences(refsToAddAndStudyElementEntities);
        removeStudyElementReferences(parentStudyModuleId, organisationId, referencesStudyElements, subElements);
    }

    private List<AbstractStudyElementWriteDTO> getStudyElementsWithAddedOrUpdatedReferences(List<AbstractStudyElementWriteDTO> subElements,
                                                                            List<StudyElementEntity> referencesStudyElements) {
        List<AbstractStudyElementWriteDTO> newOrUpdatedReferences = new ArrayList<>();
        for(AbstractStudyElementWriteDTO subElement : subElements) {
            Boolean onlyEnrollableWhenParent = null;
            if(subElement instanceof CourseUnitWriteDTO) {
                onlyEnrollableWhenParent = ((CourseUnitWriteDTO) subElement).getOnlyEnrollableWithParent();
            }

            Boolean finalOnlyEnrollableWhenParent = onlyEnrollableWhenParent;

            boolean parentReferenceExists = referencesStudyElements.stream()
                .anyMatch(existingRef -> existingRef.getParents().stream().anyMatch(
                        existingParent -> existingParent.getReferenceIdentifier().equals(subElement.getStudyElementId())
                                && existingParent.getOnlyEnrollableWithParent() == finalOnlyEnrollableWhenParent
                                && existingParent.getReferenceOrganizer().equals(subElement.getOrganizingOrganisationId())
                                && existingParent.getReferenceType() == StudyElementType.STUDY_MODULE)
                );

            if(!parentReferenceExists) {
                newOrUpdatedReferences.add(subElement);
            }
        }

        return newOrUpdatedReferences;
    }

    // Could throw exceptions so do not save anything to elastic before calling this method
    private HashMap<StudyElementEntity, StudyElementReference> getReferencesAndStudyElementsToAddRefTo(String parentStudyModuleId,
                                                                                                       String organisationId,
                                                                                                       List<CooperationNetwork> parentNetworks,
                                                                                                       List<AbstractStudyElementWriteDTO> newOrUpdatedReference) {
        HashMap<StudyElementEntity, StudyElementReference> refsToAddAndStudyElementEntities = new HashMap<>();
        for(AbstractStudyElementWriteDTO newReference : newOrUpdatedReference) {
            StudyElementEntity entity = (StudyElementEntity) getRepositoryForStudyElementWriteDTO(newReference).findByStudyElementIdAndOrganizingOrganisationId(
                    newReference.getStudyElementId(), newReference.getOrganizingOrganisationId())
                    .orElse(null);

            if (entity == null) {
                throw new StudyElementEntityNotFoundException("Unable to handle update study module. Message contains " + newReference.getType()
                    + " sub element that does not exist: [" + newReference.getStudyElementId() + ", " + newReference.getOrganizingOrganisationId() + "]");
            }

            if(!CollectionUtils.isEmpty(parentNetworks)) {
                boolean newReferencePartOfSameNetwork = false;
                if (!CollectionUtils.isEmpty(entity.getCooperationNetworks())) {
                    List<NetworkEntity> organisationNetworks = networkRepository.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged());
                    newReferencePartOfSameNetwork = entity.getCooperationNetworks().stream().anyMatch(network ->
                        organisationNetworks.stream().anyMatch(orgNet -> orgNet.getId().equals(network.getId())));
                }

                if (!newReferencePartOfSameNetwork) {
                    throw new StudyElementReferenceNotInSameNetworkValidationException("Unable to handle update study module. " +
                        "Message contains " + newReference.getType() + " sub element that does not belong to same network: " +
                        "[" + newReference.getStudyElementId() + ", " + newReference.getOrganizingOrganisationId() + "]");
                }
            }

            StudyElementReference parentReference = getParentReference(parentStudyModuleId, organisationId, StudyElementType.STUDY_MODULE, newReference);
            refsToAddAndStudyElementEntities.put(entity, parentReference);
        }

        return refsToAddAndStudyElementEntities;
    }

    private void upsertStudyElementReferences(HashMap<StudyElementEntity, StudyElementReference> refsToAddAndStudyElementEntities) {
        for(Map.Entry<StudyElementEntity, StudyElementReference> refToAddAndStudyElementEntity : refsToAddAndStudyElementEntities.entrySet()) {
            StudyElementEntity entityToUpdate = refToAddAndStudyElementEntity.getKey();
            StudyElementReference studyElementReference = refToAddAndStudyElementEntity.getValue();

            getRepositoryForEntityClass(entityToUpdate.getClass()).saveHistory(entityToUpdate, entityToUpdate.getClass());

            if(entityToUpdate.getParents() == null) {
                entityToUpdate.setParents(new ArrayList<>());
            }

            entityToUpdate.getParents().removeIf(
                p -> p.getReferenceIdentifier().equals(studyElementReference.getReferenceIdentifier())
                    && p.getReferenceType().equals(StudyElementType.STUDY_MODULE)
                    && p.getReferenceOrganizer().equals(studyElementReference.getReferenceOrganizer()));

            entityToUpdate.getParents().add(studyElementReference);
            getRepositoryForEntityClass(entityToUpdate.getClass()).update(entityToUpdate);
        }
    }

    private void removeStudyElementReferences(String parentStudyModuleId, String organisationId, List<StudyElementEntity> referencesStudyElements,
                                              List<AbstractStudyElementWriteDTO> finalSubElements) {
        List<StudyElementEntity> refRemovedStudyElements = referencesStudyElements.stream()
                .filter(se -> finalSubElements.stream().noneMatch(
                            sub -> sub.getStudyElementId().equals(se.getStudyElementId())
                                && sub.getOrganizingOrganisationId().equals(se.getOrganizingOrganisationId())))
                .collect(Collectors.toList());

        // Delete removed references
        for(StudyElementEntity refRemoved : refRemovedStudyElements) {
            Predicate<StudyElementReference> removePredicate =
                    p -> p.getReferenceIdentifier().equals(parentStudyModuleId)
                            && p.getReferenceType().equals(StudyElementType.STUDY_MODULE)
                            && p.getReferenceOrganizer().equals(organisationId);

            getRepositoryForEntityClass(refRemoved.getClass()).saveHistory(refRemoved, refRemoved.getClass());
            refRemoved.getParents().removeIf(removePredicate);
            getRepositoryForEntityClass(refRemoved.getClass()).update(refRemoved);
        }
    }
}
