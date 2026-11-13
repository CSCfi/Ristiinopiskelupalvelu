package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.entity.common.AddressEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.PhoneEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "koulut", createIndex = false)
public class OrganisationEntity extends GenericEntity implements Serializable {

    private String organisationIdentifier;
    private LocalisedStringEntity organisationName;
    private LocalisedStringEntity unitName;
    private LocalisedStringEntity nameDescription;
    private String municipalityCode;
    private String url;
    private AddressEntity postalAddress;
    private AddressEntity streetAddress;
    private PhoneEntity phone;
    private String queue;
    private String administratorEmail;
    private boolean notificationsEnabled = false;
    private int schemaVersion;

    public static OrganisationEntity from(Organisation source) {
        if (source == null) {
            return null;
        }

        OrganisationEntity target = new OrganisationEntity();
        target.setId(source.getOrganisationIdentifier() != null ? source.getOrganisationIdentifier() : source.getOrganisationTkCode());
        target.setQueue(source.getOrganisationTkCode());
        target.setOrganisationIdentifier(source.getOrganisationIdentifier());
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

    public static OrganisationEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation source) {
        if (source == null) {
            return null;
        }

        OrganisationEntity target = new OrganisationEntity();
        target.setId(source.getOrganisationIdentifier() != null ? source.getOrganisationIdentifier() : source.getOrganisationTkCode());
        target.setQueue(source.getOrganisationTkCode());
        target.setOrganisationIdentifier(source.getOrganisationIdentifier());
        target.setOrganisationName(LocalisedStringEntity.from(source.getOrganisationName()));
        target.setUnitName(LocalisedStringEntity.from(source.getUnitName()));
        target.setNameDescription(LocalisedStringEntity.from(source.getNameDescription()));
        target.setMunicipalityCode(source.getMunicipalityCode());
        target.setUrl(source.getUrl());
        return target;
    }
}
