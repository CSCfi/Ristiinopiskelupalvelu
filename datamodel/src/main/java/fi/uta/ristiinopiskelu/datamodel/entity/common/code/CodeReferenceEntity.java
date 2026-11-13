package fi.uta.ristiinopiskelu.datamodel.entity.common.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeReferenceEntity {

    private String key;
    private String codeSetKey;

    public static CodeReferenceEntity from(CodeReference source) {
        if (source == null) {
            return null;
        }
        return new CodeReferenceEntity(source.getKey(), source.getCodeSetKey());
    }

    public static List<CodeReferenceEntity> from(List<CodeReference> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(CodeReferenceEntity::from).toList();
    }
}
