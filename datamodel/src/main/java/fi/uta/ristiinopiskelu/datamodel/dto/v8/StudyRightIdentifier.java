package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyRightIdentifier {

    private String organisationTkCodeReference;
    private String studyRightId;
}
