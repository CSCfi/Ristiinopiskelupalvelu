package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.PersonReference;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeReferenceEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonReferenceEntity {

    private CodeReferenceEntity personRole;
    private PersonEntity person = null;
    private String definition = null;

    public static PersonReferenceEntity from(PersonReference source) {
        if (source == null) {
            return null;
        }
        PersonReferenceEntity target = new PersonReferenceEntity();
        target.setPersonRole(CodeReferenceEntity.from(source.getPersonRole()));
        target.setPerson(PersonEntity.from(source.getPerson()));
        target.setDefinition(source.getDefinition());
        return target;
    }

    public static List<PersonReferenceEntity> from(List<PersonReference> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(PersonReferenceEntity::from).toList();
    }
}
