package fi.uta.ristiinopiskelu.datamodel.entity.common.code;

import fi.uta.ristiinopiskelu.datamodel.entity.common.LanguageEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeValueEntity {

    private LanguageEntity language;
    private String value;
    private String abbreviation;
    private String description;
}
