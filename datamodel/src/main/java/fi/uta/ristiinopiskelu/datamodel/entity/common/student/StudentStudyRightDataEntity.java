package fi.uta.ristiinopiskelu.datamodel.entity.common.student;

import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightDataEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StudentStudyRightDataEntity extends StudyRightDataEntity {

    private Boolean eligibleForNetworkStudies;
}
