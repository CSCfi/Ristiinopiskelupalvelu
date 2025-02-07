package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.CourseUnitReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Realisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

public class RealisationSearchParameters extends PageableSearchParameters<Realisation> {

    @Schema(description = "Rajaa toteutuksia tunnisteen mukaan.")
    private String realisationId;

    @Schema(description = "Rajaa toteutuksia tunnistekoodin mukaan.")
    private String realisationIdentifierCode;

    @Schema(description = "Rajaa toteutuksia järjestäjien mukaan.")
    private String organizingOrganisationId;

    @Schema(description = "Rajaa toteutuksia opintojaksoviitteiden ja niitä tarjoavien organisaatioiden perusteella.")
    private List<CourseUnitReference> courseUnitReferences;

    @Schema(description = "Rajaa toteutuksia verkostojen mukaan.")
    private List<String> networkIdentifiers;

    @Schema(description = "Rajaa tarjontaa nimen tai koodin perusteella.")
    private String query;

    @Schema(description = "Palautetaanko vain toteutukset, joissa endDate > nykyinen ajanhetki. Oletusarvo: true")
    private Boolean includePast = true;

    @Schema(description = "Kieli, jolle sanahaku kohdistuu. Mahdolliset arvot: fi, sv, en. Oletusarvo: fi")
    private Language language = Language.FI;

    @Schema(description = "Haetaanko vain toteutuksia, joissa on ilmoittautuminen käynnissä. Oletusarvo: true")
    private Boolean ongoingEnrollment = true;

    @Schema(description = "Palautetaanko sellaisita toteutuksia, joiden verkosto tai verkostoliitos ei ole voimassa. Oletusarvo: false")
    private boolean includeInactive = false;

    @Schema(description = "Palautetaanko omia toteutuksia. Oletusarvo: false")
    private boolean includeOwn = false;

    @Schema(description = "Rajaa toteutuksia niiden tilojen mukaan. Oletuksena palautetaan vain aktiiviset.")
    private List<StudyStatus> statuses = Arrays.asList(StudyStatus.ACTIVE);

    @Schema(description = "Rajaa toteutuksia koulutusalojen mukaan. Oletuksena palautetaan kaikki.")
    private List<MinEduGuidanceArea> minEduGuidanceAreas;

    public RealisationSearchParameters() {
        
    }

    public RealisationSearchParameters(String realisationId, String realisationIdentifierCode, String organizingOrganisationId) {
        this.realisationId = realisationId;
        this.realisationIdentifierCode = realisationIdentifierCode;
        this.organizingOrganisationId = organizingOrganisationId;
    }

    public String getRealisationId() {
        return realisationId;
    }

    public void setRealisationId(String realisationId) {
        this.realisationId = realisationId;
    }

    public String getRealisationIdentifierCode() {
        return realisationIdentifierCode;
    }

    public void setRealisationIdentifierCode(String realisationIdentifierCode) {
        this.realisationIdentifierCode = realisationIdentifierCode;
    }

    public String getOrganizingOrganisationId() {
        return organizingOrganisationId;
    }

    public void setOrganizingOrganisationId(String organizingOrganisationId) {
        this.organizingOrganisationId = organizingOrganisationId;
    }

    public List<CourseUnitReference> getCourseUnitReferences() {
        return courseUnitReferences;
    }

    public void setCourseUnitReferences(List<CourseUnitReference> courseUnitReferences) {
        this.courseUnitReferences = courseUnitReferences;
    }

    public boolean isOngoingEnrollment() {
        return ongoingEnrollment != null ? ongoingEnrollment : true;
    }

    public void setOngoingEnrollment(boolean ongoingEnrollment) {
        this.ongoingEnrollment = ongoingEnrollment;
    }

    public List<String> getNetworkIdentifiers() {
        return networkIdentifiers;
    }

    public void setNetworkIdentifiers(List<String> networkIdentifiers) {
        this.networkIdentifiers = networkIdentifiers;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Language getLanguage() {
        return language != null ? language : Language.FI;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public boolean isIncludeInactive() {
        return includeInactive;
    }

    public void setIncludeInactive(boolean includeInactive) {
        this.includeInactive = includeInactive;
    }

    public boolean isIncludeOwn() {
        return includeOwn;
    }

    public void setIncludeOwn(boolean includeOwn) {
        this.includeOwn = includeOwn;
    }

    public boolean isIncludePast() {
        return includePast != null ? includePast : true;
    }

    public void setIncludePast(boolean includePast) {
        this.includePast = includePast;
    }

    public List<StudyStatus> getStatuses() {
        return statuses != null ? statuses : Arrays.asList(StudyStatus.ACTIVE);
    }

    public void setStatuses(List<StudyStatus> statuses) {
        this.statuses = statuses;
    }

    public List<MinEduGuidanceArea> getMinEduGuidanceAreas() {
        return minEduGuidanceAreas;
    }

    public void setMinEduGuidanceAreas(List<MinEduGuidanceArea> minEduGuidanceAreas) {
        this.minEduGuidanceAreas = minEduGuidanceAreas;
    }
}
