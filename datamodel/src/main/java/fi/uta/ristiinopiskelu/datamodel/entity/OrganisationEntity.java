package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Document(indexName = "koulut", createIndex = false)
public class OrganisationEntity extends GenericEntity implements Serializable {
    private String organisationIdentifier;
    private LocalisedString organisationName;
    private LocalisedString unitName;
    private LocalisedString nameDescription;
    private String municipalityCode;
    private String url;
    private Address postalAddress;
    private Address streetAddress;
    private Phone phone;
    private String queue;
    private String administratorEmail;
    private boolean notificationsEnabled = false;
    private int schemaVersion;

    public void setOrganisationName(LocalisedString organisationName) {
        this.organisationName = organisationName;
    }

    public void setUnitName(LocalisedString unitName) {
        this.unitName = unitName;
    }

    public void setNameDescription(LocalisedString nameDescription) {
        this.nameDescription = nameDescription;
    }

    public void setPostalAddress(Address postalAddress) {
        this.postalAddress = postalAddress;
    }

    public void setStreetAddress(Address streetAddress) {
        this.streetAddress = streetAddress;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }

    public LocalisedString getOrganisationName() {
        return this.organisationName;
    }

    public LocalisedString getUnitName() {
        return this.unitName;
    }

    public LocalisedString getNameDescription() {
        return this.nameDescription;
    }

    public Address getPostalAddress() {
        return this.postalAddress;
    }

    public Address getStreetAddress() {
        return this.streetAddress;
    }

    public Phone getPhone() {
        return this.phone;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getAdministratorEmail() {
        return administratorEmail;
    }

    public void setAdministratorEmail(String administratorEmail) {
        this.administratorEmail = administratorEmail;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getOrganisationIdentifier() {
        return organisationIdentifier;
    }

    public void setOrganisationIdentifier(String organisationIdentifier) {
        this.organisationIdentifier = organisationIdentifier;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public void setMunicipalityCode(String municipalityCode) {
        this.municipalityCode = municipalityCode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public OrganisationEntity() {
    }

    public String toString() {
        return "Organisation(organisationName=" + this.getOrganisationName() + ", unitName=" + this.getUnitName() + ", nameDescription=" + this.getNameDescription() + ", postalAddress=" + this.getPostalAddress() + ", streetAddress=" + this.getStreetAddress() + ", phone=" + this.getPhone() + ")";
    }
}
