package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.search;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.PageableSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyElementRestDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

public class StudyElementSearchParameters<T extends StudyElementRestDTO> extends PageableSearchParameters<T> {

    @Schema(description = "Tunniste")
    private String studyElementId;

    @Schema(description = "Tunnistekoodi")
    private String studyElementIdentifierCode;

    @Schema(description = "Järjestävä oragnisaatio")
    private String organizingOrganisationId;

    @Schema(description = "Rajaa elementtejä tilojen mukaan. Oletuksena palautetaan vain aktiiviset.")
    private List<StudyStatus> statuses = Arrays.asList(StudyStatus.ACTIVE);

    @Schema(description = "Palautetaanko vain voimassaolevaa tarjontaa voimassaolevista verkostoista. Oletusarvo: false")
    private boolean includeInactive = false;

    public StudyElementSearchParameters() {
        
    }

    public StudyElementSearchParameters(String studyElementId, String studyElementIdentifierCode,
                                        String organizingOrganisationId, List<StudyStatus> statuses, boolean includeInactive,
                                        int page, int pageSize) {
        this.studyElementId = studyElementId;
        this.studyElementIdentifierCode = studyElementIdentifierCode;
        this.organizingOrganisationId = organizingOrganisationId;
        this.statuses = statuses;
        this.includeInactive = includeInactive;
        this.setPage(page);
        this.setPageSize(pageSize);
    }

    public String getStudyElementId() {
        return studyElementId;
    }

    public void setStudyElementId(String studyElementId) {
        this.studyElementId = studyElementId;
    }

    public String getStudyElementIdentifierCode() {
        return studyElementIdentifierCode;
    }

    public void setStudyElementIdentifierCode(String studyElementIdentifierCode) {
        this.studyElementIdentifierCode = studyElementIdentifierCode;
    }

    public String getOrganizingOrganisationId() {
        return organizingOrganisationId;
    }

    public void setOrganizingOrganisationId(String organizingOrganisationId) {
        this.organizingOrganisationId = organizingOrganisationId;
    }

    public List<StudyStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<StudyStatus> statuses) {
        this.statuses = statuses;
    }

    public boolean isIncludeInactive() {
        return includeInactive;
    }

    public void setIncludeInactive(boolean includeInactive) {
        this.includeInactive = includeInactive;
    }
}
