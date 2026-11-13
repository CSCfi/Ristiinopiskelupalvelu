package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.MinEduGuidanceAreaEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "toteutukset", createIndex = false)
public class RealisationEntity extends CompositeIdentifiedEntity implements Serializable {

    private String realisationId;
    private String realisationIdentifierCode;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime createdTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime updateTime;

    private List<StudyElementReferenceEntity> studyElementReferences;
    private List<SelectionEntity> selections;
    private List<QuestionSetEntity> questionSets;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentStartDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentEndDateTime;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    private List<PersonReferenceEntity> personReferences;
    private LocalisedStringEntity name;
    private Integer minSeats;
    private Integer maxSeats;
    private AddressEntity location;
    private List<DescriptionEntity> organisationSpecificDescriptions;
    private List<OrganisationReferenceEntity> organisationReferences;
    private String organizingOrganisationId;
    private List<SelectionEntity> groupSelections;
    private List<CooperationNetworkEntity> cooperationNetworks;
    private List<GroupQuotaEntity> groupQuotas;
    private StudyStatusEntity status;
    private BigDecimal creditsMin;
    private BigDecimal creditsMax;
    private List<String> teachingLanguage;
    private List<MinEduGuidanceAreaEntity> minEduGuidanceArea;
    private boolean enrollmentClosed = false;

    @JsonIgnore
    @Transient
    @Override
    public String getElementId() {
        return realisationId;
    }

    @JsonIgnore
    @Transient
    @Override
    public CompositeIdentifiedEntityTypeEntity getType() {
        return CompositeIdentifiedEntityTypeEntity.REALISATION;
    }

    public static RealisationEntity from(RealisationWriteDTO source) {
        if (source == null) {
            return null;
        }

        RealisationEntity target = new RealisationEntity();
        target.setRealisationId(source.getRealisationId());
        target.setRealisationIdentifierCode(source.getRealisationIdentifierCode());
        target.setSelections(SelectionEntity.from(source.getSelections()));
        target.setQuestionSets(QuestionSetEntity.from(source.getQuestionSets()));
        target.setEnrollmentStartDateTime(source.getEnrollmentStartDateTime());
        target.setEnrollmentEndDateTime(source.getEnrollmentEndDateTime());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setPersonReferences(PersonReferenceEntity.from(source.getPersonReferences()));
        target.setMinSeats(source.getMinSeats());
        target.setMaxSeats(source.getMaxSeats());
        target.setLocation(AddressEntity.from(source.getLocation()));
        target.setOrganisationSpecificDescriptions(DescriptionEntity.from(source.getOrganisationSpecificDescriptions()));
        target.setStudyElementReferences(StudyElementReferenceEntity.from(source.getStudyElementReferences()));
        target.setName(LocalisedStringEntity.from(source.getName()));
        target.setOrganisationReferences(OrganisationReferenceEntity.from(source.getOrganisationReferences()));
        target.setGroupSelections(SelectionEntity.from(source.getGroupSelections()));
        target.setCooperationNetworks(CooperationNetworkEntity.from(source.getCooperationNetworks()));
        target.setCreatedTime(source.getCreatedTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setGroupQuotas(GroupQuotaEntity.from(source.getGroupQuotas()));
        target.setStatus(source.getStatus() == null ? null : StudyStatusEntity.valueOf(source.getStatus().name()));
        target.setCreditsMin(source.getCreditsMin());
        target.setCreditsMax(source.getCreditsMax());
        target.setTeachingLanguage(source.getTeachingLanguage());
        target.setMinEduGuidanceArea(source.getMinEduGuidanceArea() == null ? null
            : source.getMinEduGuidanceArea().stream().map(value -> MinEduGuidanceAreaEntity.valueOf(value.name())).toList());
        target.setEnrollmentClosed(source.isEnrollmentClosed());
        target.setOrganizingOrganisationId(source.getOrganizingOrganisationId());
        return target;
    }
}
