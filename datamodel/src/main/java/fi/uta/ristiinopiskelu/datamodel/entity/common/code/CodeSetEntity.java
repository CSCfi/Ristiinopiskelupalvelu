package fi.uta.ristiinopiskelu.datamodel.entity.common.code;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeSetEntity {

    private String key;
    private String organisationOid;
    private Integer version;
}
