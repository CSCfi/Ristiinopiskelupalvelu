package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganisationDataEntity {

    private String organisationIdentifier;
    private String organisationTkCode;
    private LocalisedStringEntity organisationName;
    private LocalisedStringEntity unitName;
    private LocalisedStringEntity nameDescription;
    private AddressEntity postalAddress;
    private AddressEntity streetAddress;
    private String municipalityCode;
    private PhoneEntity phone;
    private String url;

    public static OrganisationDataEntity from(Organisation source) {
        if (source == null) {
            return null;
        }

        OrganisationDataEntity target = new OrganisationDataEntity();
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
        return target;
    }
}
