package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.search.StudyElementSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO;

import java.util.List;

public class CourseUnitSearchParameters extends StudyElementSearchParameters<CourseUnitRestDTO> {

    public CourseUnitSearchParameters() {
        super();
    }

    public CourseUnitSearchParameters(String studyElementId, String studyElementIdentifierCode,
                                      String organizingOrganisationId, List<StudyStatus> statuses, boolean includeInactive,
                                      int page, int pageSize) {
        super(studyElementId, studyElementIdentifierCode, organizingOrganisationId, statuses, includeInactive, page, pageSize);
    }
}
