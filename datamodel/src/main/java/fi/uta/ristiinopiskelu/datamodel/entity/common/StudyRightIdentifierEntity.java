package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyRightIdentifierEntity {

    private String organisationTkCodeReference;
    private String studyRightId;

    public static StudyRightIdentifierEntity from(StudyRightIdentifier source) {
        if (source == null) {
            return null;
        }

        return new StudyRightIdentifierEntity(source.getOrganisationTkCodeReference(), source.getStudyRightId());
    }
}
