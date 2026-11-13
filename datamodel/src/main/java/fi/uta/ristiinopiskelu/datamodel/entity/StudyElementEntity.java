package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.ExternalCodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.degree.DegreeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.ExternalCodeReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.MinEduGuidanceAreaEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = StudyModuleEntity.class, name = "STUDY_MODULE"),
    @JsonSubTypes.Type(value = CourseUnitEntity.class, name = "COURSE_UNIT"),
    @JsonSubTypes.Type(value = DegreeEntity.class, name = "DEGREE")
})
public abstract class StudyElementEntity extends CompositeIdentifiedEntity implements Serializable {
    private Boolean missing;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime sendingTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime createdTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime updateTime;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityStartDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityEndDate;

    private String studyElementPermanentId;
    private String studyElementId;
    private String studyElementIdentifierCode;
    private List<ExternalCodeReferenceEntity> classificationCodes;
    private String abbreviation;
    private LocalisedStringEntity name;
    private LocalisedStringEntity description;
    private BigDecimal creditsMin;
    private BigDecimal creditsMax;
    private LocalisedStringEntity creditsDescription;
    private List<OrganisationReferenceEntity> organisationReferences;
    private List<PersonReferenceEntity> personReferences;
    private List<KeywordEntity> keywords;
    private LocalisedStringEntity preconditions;
    private List<String> teachingLanguage;
    private List<String> languagesOfCompletion;
    private LocalisedStringEntity furtherInformation;
    private List<CodeReferenceEntity> targetGroups;
    private List<CooperationNetworkEntity> cooperationNetworks;
    private LocalisedStringEntity objective;
    private LocalisedStringEntity content;
    private List<DescriptionEntity> organisationSpecificDescriptions;
    private String organizingOrganisationId;
    private List<StudyElementReferenceEntity> parents;
    private StudyStatusEntity status;
    private List<MinEduGuidanceAreaEntity> minEduGuidanceArea;
    private CompositeIdentifiedEntityTypeEntity type;

    @Override
    public List<CooperationNetworkEntity> getCooperationNetworks() {
        return this.cooperationNetworks;
    }

    @Override
    public String getOrganizingOrganisationId() {
        return organizingOrganisationId;
    }

    @Override
    public void setOrganizingOrganisationId(String organizingOrganisationId) {
        this.organizingOrganisationId = organizingOrganisationId;
    }

    @Override
    public CompositeIdentifiedEntityTypeEntity getType() {
        return type;
    }

    @JsonIgnore
    @Transient
    @Override
    public String getElementId() {
        return studyElementId;
    }

    public static StudyElementEntity from(AbstractStudyElementWriteDTO source) {
        if (source instanceof CourseUnitWriteDTO courseUnitWriteDTO) {
            return CourseUnitEntity.from(courseUnitWriteDTO);
        }
        if (source instanceof StudyModuleWriteDTO studyModuleWriteDTO) {
            return StudyModuleEntity.from(studyModuleWriteDTO);
        }
        if (source instanceof DegreeWriteDTO degreeWriteDTO) {
            return DegreeEntity.from(degreeWriteDTO);
        }

        if (source == null) {
            return null;
        }

        if (source.getType() == StudyElementType.COURSE_UNIT) {
            return new CourseUnitEntity();
        }
        if (source.getType() == StudyElementType.STUDY_MODULE) {
            return new StudyModuleEntity();
        }
        if (source.getType() == StudyElementType.DEGREE) {
            return new DegreeEntity();
        }

        throw new IllegalStateException("Unidentified StudyElement DTO type: " + source.getClass().getSimpleName());
    }

    public static <T extends AbstractStudyElementReadDTO> void copyStudyElementEntityToReadDto(StudyElementEntity source, T target) {
        target.setMissing(source.getMissing());
        target.setSendingTime(source.getSendingTime());
        target.setCreatedTime(source.getCreatedTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setValidityStartDate(source.getValidityStartDate());
        target.setValidityEndDate(source.getValidityEndDate());
        target.setStudyElementId(source.getStudyElementId());
        target.setStudyElementPermanentId(source.getStudyElementPermanentId());
        target.setStudyElementIdentifierCode(source.getStudyElementIdentifierCode());
        target.setClassificationCodes(ExternalCodeReference.from(source.getClassificationCodes()));
        target.setAbbreviation(source.getAbbreviation());
        target.setName(LocalisedString.from(source.getName()));
        target.setDescription(LocalisedString.from(source.getDescription()));
        target.setCreditsMin(source.getCreditsMin());
        target.setCreditsMax(source.getCreditsMax());
        target.setCreditsDescription(LocalisedString.from(source.getCreditsDescription()));
        target.setOrganisationReferences(OrganisationReference.from(source.getOrganisationReferences()));
        target.setPersonReferences(PersonReference.from(source.getPersonReferences()));
        target.setKeywords(Keyword.from(source.getKeywords()));
        target.setPreconditions(LocalisedString.from(source.getPreconditions()));
        target.setTeachingLanguage(source.getTeachingLanguage());
        target.setLanguagesOfCompletion(source.getLanguagesOfCompletion());
        target.setFurtherInformation(LocalisedString.from(source.getFurtherInformation()));
        target.setTargetGroups(CodeReference.from(source.getTargetGroups()));
        target.setCooperationNetworks(CooperationNetwork.from(source.getCooperationNetworks()));
        target.setObjective(LocalisedString.from(source.getObjective()));
        target.setContent(LocalisedString.from(source.getContent()));
        target.setOrganisationSpecificDescriptions(Description.from(source.getOrganisationSpecificDescriptions()));
        target.setParents(StudyElementReference.from(source.getParents()));
        target.setStatus(StudyStatus.from(source.getStatus()));
        target.setMinEduGuidanceArea(MinEduGuidanceArea.from(source.getMinEduGuidanceArea()));
        target.setType(StudyElementType.from(source.getType()));
    }
}
