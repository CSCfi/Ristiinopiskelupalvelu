package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyStatusEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.MinEduGuidanceAreaEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseUnitRealisationEntity {
    private String realisationId;
    private String realisationIdentifierCode;
    private String organizingOrganisationId;
    private LocalisedStringEntity name = null;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentStartDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentEndDateTime;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate = null;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate = null;

    private boolean enrollmentClosed = false;
    private List<CooperationNetworkEntity> cooperationNetworks;
    private List<String> teachingLanguage;
    private StudyStatusEntity status;
    private List<MinEduGuidanceAreaEntity> minEduGuidanceArea;

    public static CourseUnitRealisationEntity from(RealisationEntity source) {
        if (source == null) {
            return null;
        }

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

    public static CourseUnitRealisationEntity from(RealisationWriteDTO source) {
        if (source == null) {
            return null;
        }

        CourseUnitRealisationEntity target = new CourseUnitRealisationEntity();
        target.setRealisationId(source.getRealisationId());
        target.setRealisationIdentifierCode(source.getRealisationIdentifierCode());
        target.setEnrollmentStartDateTime(source.getEnrollmentStartDateTime());
        target.setEnrollmentEndDateTime(source.getEnrollmentEndDateTime());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setName(LocalisedStringEntity.from(source.getName()));
        target.setCooperationNetworks(CooperationNetworkEntity.from(source.getCooperationNetworks()));
        target.setStatus(StudyStatusEntity.from(source.getStatus()));
        target.setTeachingLanguage(source.getTeachingLanguage());
        target.setMinEduGuidanceArea(MinEduGuidanceAreaEntity.from(source.getMinEduGuidanceArea()));
        target.setEnrollmentClosed(source.isEnrollmentClosed());
        target.setOrganizingOrganisationId(source.getOrganizingOrganisationId());
        return target;
    }

    public static List<CourseUnitRealisationEntity> from(List<RealisationWriteDTO> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .map(CourseUnitRealisationEntity::from)
            .toList();
    }
}
