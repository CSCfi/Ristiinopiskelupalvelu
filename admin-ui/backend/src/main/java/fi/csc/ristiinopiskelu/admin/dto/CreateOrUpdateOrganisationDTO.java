package fi.csc.ristiinopiskelu.admin.dto;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;

public class CreateOrUpdateOrganisationDTO {

    private String id;
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
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganisationIdentifier() {
        return organisationIdentifier;
    }

    public void setOrganisationIdentifier(String organisationIdentifier) {
        this.organisationIdentifier = organisationIdentifier;
    }

    public LocalisedString getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(LocalisedString organisationName) {
        this.organisationName = organisationName;
    }

    public LocalisedString getUnitName() {
        return unitName;
    }

    public void setUnitName(LocalisedString unitName) {
        this.unitName = unitName;
    }

    public LocalisedString getNameDescription() {
        return nameDescription;
    }

    public void setNameDescription(LocalisedString nameDescription) {
        this.nameDescription = nameDescription;
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

    public Address getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(Address postalAddress) {
        this.postalAddress = postalAddress;
    }

    public Address getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(Address streetAddress) {
        this.streetAddress = streetAddress;
    }

    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
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

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
