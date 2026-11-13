package fi.uta.ristiinopiskelu.datamodel.entity.common.student;

import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStatusEntity {

    private String organisationId;
    private StudentStatusEntity status;
    private LocalisedStringEntity rejectionReason;
}
