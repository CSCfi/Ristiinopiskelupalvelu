package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightIdentifierEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyRightIdentifier {

    private String organisationTkCodeReference;
    private String studyRightId;

    public static StudyRightIdentifier from(StudyRightIdentifierEntity identifier) {
        return new StudyRightIdentifier(identifier.getOrganisationTkCodeReference(), identifier.getStudyRightId());
    }
}
