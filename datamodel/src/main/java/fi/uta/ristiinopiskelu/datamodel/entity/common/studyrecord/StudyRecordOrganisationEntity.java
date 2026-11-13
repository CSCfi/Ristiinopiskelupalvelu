package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordOrganisation;
import fi.uta.ristiinopiskelu.datamodel.entity.common.OrganisationDataEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.AddressEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.PhoneEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StudyRecordOrganisationEntity extends OrganisationDataEntity {

    private Integer PIC;
    private String erasmusCode;
    private String HEI;
    private String erasmusCharterCode;
    private Integer erasmusOrganisationStatisticsCode;
    private String erasmusOrganisationName;

    public static StudyRecordOrganisationEntity from(StudyRecordOrganisation source) {
        if (source == null) {
            return null;
        }
        StudyRecordOrganisationEntity target = new StudyRecordOrganisationEntity();
        target.setOrganisationIdentifier(source.getOrganisationIdentifier());
        target.setOrganisationTkCode(source.getOrganisationTkCode());
        target.setOrganisationName(LocalisedStringEntity.from(source.getOrganisationName()));
        target.setUnitName(LocalisedStringEntity.from(source.getUnitName()));
        target.setNameDescription(LocalisedStringEntity.from(source.getNameDescription()));
        target.setPostalAddress(AddressEntity.from(source.getPostalAddress()));
        target.setStreetAddress(AddressEntity.from(source.getStreetAddress()));
        target.setMunicipalityCode(source.getMunicipalityCode());
        target.setPhone(PhoneEntity.from(source.getPhone()));
        target.setUrl(source.getUrl());
        target.setPIC(source.getPIC());
        target.setErasmusCode(source.getErasmusCode());
        target.setHEI(source.getHEI());
        target.setErasmusCharterCode(source.getErasmusCharterCode());
        target.setErasmusOrganisationStatisticsCode(source.getErasmusOrganisationStatisticsCode());
        target.setErasmusOrganisationName(source.getErasmusOrganisationName());
        return target;
    }
}
