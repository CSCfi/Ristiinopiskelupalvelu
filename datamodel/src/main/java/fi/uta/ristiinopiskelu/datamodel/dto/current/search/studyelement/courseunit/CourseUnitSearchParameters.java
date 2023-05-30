package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.StudyElementSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;

import java.util.List;

public class CourseUnitSearchParameters extends StudyElementSearchParameters<CourseUnitEntity> {

    public CourseUnitSearchParameters() {
        super();
    }

    public CourseUnitSearchParameters(String studyElementId, String studyElementIdentifierCode,
                                      String organizingOrganisationId, List<StudyStatus> statuses, boolean includeInactive,
                                      int page, int pageSize) {
        super(studyElementId, studyElementIdentifierCode, organizingOrganisationId, statuses, includeInactive, page, pageSize);
    }
}
