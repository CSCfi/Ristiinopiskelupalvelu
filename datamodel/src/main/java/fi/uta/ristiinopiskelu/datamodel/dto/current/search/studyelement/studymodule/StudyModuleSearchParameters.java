package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.StudyElementSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;

import java.util.List;

public class StudyModuleSearchParameters extends StudyElementSearchParameters<StudyModuleEntity> {

    public StudyModuleSearchParameters() {
        super();
    }

    public StudyModuleSearchParameters(String studyElementId, String studyElementIdentifierCode,
                                       String organizingOrganisationId, List<StudyStatus> statuses, boolean includeInactive,
                                       int page, int pageSize) {
        super(studyElementId, studyElementIdentifierCode, organizingOrganisationId, statuses, includeInactive, page, pageSize);
    }
}
