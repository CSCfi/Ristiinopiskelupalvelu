package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.search.StudyElementSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO;

import java.util.List;

public class StudyModuleSearchParameters extends StudyElementSearchParameters<StudyModuleRestDTO> {

    public StudyModuleSearchParameters() {
        super();
    }

    public StudyModuleSearchParameters(String studyElementId, String studyElementIdentifierCode,
                                       String organizingOrganisationId, List<StudyStatus> statuses, boolean includeInactive,
                                       int page, int pageSize) {
        super(studyElementId, studyElementIdentifierCode, organizingOrganisationId, statuses, includeInactive, page, pageSize);
    }
}
