package fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.AssessmentItemEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.impl.AbstractStudyElementService;
import fi.uta.ristiinopiskelu.handler.validator.JsonRequestValidator;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.AbstractStudyElementValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UpdateCourseUnitValidator extends AbstractStudyElementValidator<CourseUnitWriteDTO> implements JsonRequestValidator<CourseUnitEntity> {

    private static final Logger logger = LoggerFactory.getLogger(UpdateCourseUnitValidator.class);

    private RealisationService realisationService;
    private ObjectMapper objectMapper;

    @Autowired
    public UpdateCourseUnitValidator(List<AbstractStudyElementService> studyElementServices,
                                     NetworkService networkService,
                                     RealisationService realisationService,
                                     ObjectMapper objectMapper, Validator beanValidator) {
        super(studyElementServices, networkService, beanValidator);
        this.realisationService = realisationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public CourseUnitEntity validateJson(JsonNode requestJson, String organisationId) throws ValidationException {
        if(StringUtils.isEmpty(organisationId)) {
            throw new InvalidMessageHeaderException("Cannot perform update. Organisation Id is missing from header. This should not happen.");
        }

        if(requestJson == null || !requestJson.hasNonNull("courseUnit")) {
            throw new InvalidMessageBodyException("Received update course unit request -message without courseUnit -object.");
        }

        JsonNode courseUnitJson = requestJson.get("courseUnit");

        if(!courseUnitJson.hasNonNull("type")) {
            ((ObjectNode)courseUnitJson).put("type", StudyElementType.COURSE_UNIT.name());
        }

        CourseUnitWriteDTO parsedCourseUnit = objectMapper.convertValue(courseUnitJson, CourseUnitWriteDTO.class);
        super.validateObject(parsedCourseUnit, organisationId);

        CourseUnitEntity originalCourseUnitEntity = getServiceForClass(parsedCourseUnit)
                .findByStudyElementIdAndOrganizingOrganisationId(parsedCourseUnit.getStudyElementId(), organisationId)
                .orElseThrow(() -> new EntityNotFoundException("Unable to update course unit. Course unit with " +
                    getStudyElementString(parsedCourseUnit) + " does not exists"));

        if(courseUnitJson.hasNonNull("parents")) {
            validateParentReferences(parsedCourseUnit.getParents());
        }

        if(courseUnitJson.has("organisationReferences")) {
            validateOrganisationReferences(parsedCourseUnit, organisationId);
        }

        if(courseUnitJson.has("cooperationNetworks")) {
            validateGivenNetworks(parsedCourseUnit, organisationId);
        }

        if(courseUnitJson.has("completionOptions")) {
            validateNoAssessmentItemsReferencingRealisationRemoved(parsedCourseUnit, originalCourseUnitEntity);
        }

        return originalCourseUnitEntity;
    }

    protected void validateNoAssessmentItemsReferencingRealisationRemoved(CourseUnitWriteDTO parsedCourseUnit, CourseUnitEntity originalCourseUnitEntity) {
        if(CollectionUtils.isEmpty(originalCourseUnitEntity.getAssessmentItems())) {
            return;
        }

        List<RealisationEntity> allExistingRealisationRefs = new ArrayList<>();
        for(AssessmentItemEntity assessmentItemEntity : originalCourseUnitEntity.getAssessmentItems()) {
            List<RealisationEntity> referencedRealisations = realisationService.findByAssessmentItemReference(
                originalCourseUnitEntity.getStudyElementId(), originalCourseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

            if(!CollectionUtils.isEmpty(referencedRealisations)) {
                allExistingRealisationRefs.addAll(referencedRealisations);
            }
        }

        if(CollectionUtils.isEmpty(allExistingRealisationRefs)) {
            return;
        }

        if(!CollectionUtils.isEmpty(allExistingRealisationRefs)
            && (CollectionUtils.isEmpty(parsedCourseUnit.getAssessmentItems()))) {
            throw new RemovedAssessmentItemHasRealisationReferenceValidationException("Unable to update course unit." +
                " Update would remove all assessment items but there exists realisations that reference this course unit" +
                " [id: " + originalCourseUnitEntity.getStudyElementId() + "] by assessment item reference.");
        }

        List<AssessmentItemEntity> removedAssessmentItems =
            originalCourseUnitEntity.getAssessmentItems().stream()
                .filter(oai -> parsedCourseUnit.getAssessmentItems().stream()
                    .noneMatch(uai -> uai.getAssessmentItemId().equals(oai.getAssessmentItemId())))
                .collect(Collectors.toList());

        List<String> removedAssessmentItemsReferencingRealisation = new ArrayList<>();
        for(AssessmentItemEntity removedAi : removedAssessmentItems) {
            for(RealisationEntity referencedRealisation : allExistingRealisationRefs) {
                for(StudyElementReference reference : referencedRealisation.getStudyElementReferences()) {
                    if (reference.getReferenceType() == StudyElementType.ASSESSMENT_ITEM
                            && reference.getReferenceIdentifier().equals(originalCourseUnitEntity.getStudyElementId())
                            && reference.getReferenceOrganizer().equals(originalCourseUnitEntity.getOrganizingOrganisationId())
                            && reference.getReferenceAssessmentItemId().equals(removedAi.getAssessmentItemId())) {
                        removedAssessmentItemsReferencingRealisation.add(getAssessmentItemReferenceString(referencedRealisation, removedAi));
                    }
                }
            }
        }

        if(!CollectionUtils.isEmpty(removedAssessmentItemsReferencingRealisation)) {
            throw new RemovedAssessmentItemHasRealisationReferenceValidationException("Unable to update course unit." +
                " Update removes assessment items referenced by existing realisations [" + String.join(",\n", removedAssessmentItemsReferencingRealisation) + "]");
        }
    }

    private String getAssessmentItemReferenceString(RealisationEntity referencedRealisation, AssessmentItemEntity removedAi) {
        return "(Removed AssessmentItemId: " + removedAi.getAssessmentItemId() +
            " references realisation " + referencedRealisation.getRealisationId() + ", " +
            referencedRealisation.getOrganizingOrganisationId() + ")";
    }

    protected void validateParentReferences(List<StudyElementReference> parents) {
        super.validateParentReferences(parents);
    }

    protected void validateOrganisationReferences(CourseUnitWriteDTO courseUnit, String organisationId) {
        super.validateOrganisationReferences(courseUnit.getStudyElementId(), courseUnit.getType(),
                courseUnit.getOrganisationReferences(), organisationId);
    }

    protected void validateGivenNetworks(CourseUnitWriteDTO courseUnit, String organisationId) {
        super.validateGivenNetworks(courseUnit, organisationId, StudyElementType.COURSE_UNIT, false);
    }

    protected CourseUnitService getServiceForClass(CourseUnitWriteDTO courseUnit) {
        return (CourseUnitService) super.getServiceForClass(courseUnit);
    }

    protected void validateSubRealisationsHaveAtLeastOneMatchingNetwork(CourseUnitWriteDTO parsedCourseUnit, String organisationId) {
        if(CollectionUtils.isEmpty(parsedCourseUnit.getCooperationNetworks())) {
            throw new CooperationNetworksMismatchValidationException("Unable to update course unit." +
                    " Update would remove all cooperationNetworks of given course unit " + getStudyElementString(parsedCourseUnit));
        }

        List<RealisationEntity> subRealisationEntities = realisationService.findByStudyElementReference(
                parsedCourseUnit.getStudyElementId(), organisationId);

        List<String> realisationsWithNoMatchingNetwork = new ArrayList<>();
        for(RealisationEntity subRealisation : subRealisationEntities) {
            if(CollectionUtils.isEmpty(subRealisation.getCooperationNetworks())) {
                // Broken data since there should never be realisation without cooperation networks
                realisationsWithNoMatchingNetwork.add("[id: " + subRealisation.getRealisationId() + "]");
                continue;
            }

            if(parsedCourseUnit.getCooperationNetworks().stream().noneMatch(
                    cuNetwork -> subRealisation.getCooperationNetworks().stream().anyMatch(
                            realisationNetwork -> realisationNetwork.getId().equals(cuNetwork.getId())))) {

                realisationsWithNoMatchingNetwork.add("[id: " + subRealisation.getRealisationId() + "]");
            }
        }

        if(!CollectionUtils.isEmpty(realisationsWithNoMatchingNetwork)) {
            throw new CooperationNetworksMismatchValidationException("Unable to update course unit " + getStudyElementString(parsedCourseUnit)
                    + " After update there would be sub realisation(s) that does not have any matching cooperation networks with parent course unit."
                    + " Realisations: " + String.join(" \n", realisationsWithNoMatchingNetwork));
        }
    }

}
