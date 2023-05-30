package fi.uta.ristiinopiskelu.datamodel.dto.current.read.organisation;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * <p>
 * Based on
 * https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
@Schema(name = "Organisation")
public class OrganisationReadDTO {

    @JsonProperty("organisationIdentifier")
    private String organisationIdentifier = null;

    @JsonProperty("organisationTkCode")
    private String organisationTkCode = null;

    @JsonProperty("organisationName")
    private LocalisedString organisationName = null;

    @JsonProperty("unitName")
    private LocalisedString unitName = null;

    @JsonProperty("nameDescription")
    private LocalisedString nameDescription = null;

    @JsonProperty("postalAddress")
    private Address postalAddress = null;

    @JsonProperty("streetAddress")
    private Address streetAddress = null;

    @JsonProperty("municipalityCode")
    private String municipalityCode = null;

    @JsonProperty("phone")
    private Phone phone = null;

    @JsonProperty("url")
    private String url = null;

    public OrganisationReadDTO() {
    }

    /**
     * @return organisationIdentifier
     **/
    public String getOrganisationIdentifier() {
        return organisationIdentifier;
    }

    public void setOrganisationIdentifier(String organisationIdentifier) {
        this.organisationIdentifier = organisationIdentifier;
    }

    @Schema(required = true, description = "Statistics Finland code for Organisation. Used as ID.")
    public String getOrganisationTkCode() {
        return organisationTkCode;
    }

    public void setOrganisationTkCode(String organisationTkCode) {
        this.organisationTkCode = organisationTkCode;
    }

    public OrganisationReadDTO organisationName(LocalisedString organisationName) {
        this.organisationName = organisationName;
        return this;
    }

    public OrganisationReadDTO addOrganisationNameItem(LocalisedString organisationNameItem) {

        this.organisationName(organisationNameItem);
        return this;
    }

    /**
     * M2 4.1.2
     *
     * @return organisationName
     **/
    @Schema(description = "M2 4.1.2", required = true)
    public LocalisedString getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(LocalisedString organisationName) {
        this.organisationName = organisationName;
    }

    public OrganisationReadDTO unitName(LocalisedString unitName) {
        this.unitName = unitName;
        return this;
    }

    public OrganisationReadDTO addUnitNameItem(LocalisedString unitNameItem) {

        if (this.unitName == null) {
            this.unitName = null;
        }

        this.unitName(unitNameItem);
        return this;
    }

    /**
     * M2 4.1.3 \"Faculty of Medicine\"
     *
     * @return unitName
     **/
    @Schema(description = "M2 4.1.3 \"Faculty of Medicine\"")
    public LocalisedString getUnitName() {
        return unitName;
    }

    public void setUnitName(LocalisedString unitName) {
        this.unitName = unitName;
    }

    /**
     * M2 4.1.4 \"Department of Pathology\"
     *
     * @return nameDescription
     **/
    @Schema(description = "M2 4.1.4 \"Department of Pathology\"")
    public LocalisedString getNameDescription() {
        return nameDescription;
    }

    public void setNameDescription(LocalisedString nameDescription) {
        this.nameDescription = nameDescription;
    }

    /**
     * Get postalAddress
     *
     * @return postalAddress
     **/
    @Schema(description = "")
    public Address getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(Address postalAddress) {
        this.postalAddress = postalAddress;
    }

    /**
     * Get streetAddress
     *
     * @return streetAddress
     **/

    @Schema(description = "")
    public Address getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(Address streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public void setMunicipalityCode(String municipalityCode) {
        this.municipalityCode = municipalityCode;
    }

    /**
     * Get phone
     *
     * @return phone
     **/
    @Schema(description = "")
    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }

    @Schema(description = "")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        OrganisationReadDTO that = (OrganisationReadDTO) o;
        return  Objects.equals(organisationIdentifier, that.organisationIdentifier) &&
                Objects.equals(organisationTkCode, that.organisationTkCode) &&
                Objects.equals(organisationName, that.organisationName) &&
                Objects.equals(unitName, that.unitName) &&
                Objects.equals(nameDescription, that.nameDescription) &&
                Objects.equals(postalAddress, that.postalAddress) &&
                Objects.equals(streetAddress, that.streetAddress) &&
                Objects.equals(municipalityCode, that.municipalityCode) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organisationIdentifier, organisationTkCode, organisationName, unitName, nameDescription, postalAddress, streetAddress, municipalityCode, phone, url);
    }

    @Override
    public String toString() {
        return "Organisation{" +
                "organisationIdentifier='" + organisationIdentifier + '\'' +
                ", organisationTkCode='" + organisationTkCode + '\'' +
                ", organisationName=" + organisationName +
                ", unitName=" + unitName +
                ", nameDescription=" + nameDescription +
                ", postalAddress=" + postalAddress +
                ", streetAddress=" + streetAddress +
                ", municipalityCode='" + municipalityCode + '\'' +
                ", phone=" + phone +
                ", url='" + url + '\'' +
                '}';
    }
}
