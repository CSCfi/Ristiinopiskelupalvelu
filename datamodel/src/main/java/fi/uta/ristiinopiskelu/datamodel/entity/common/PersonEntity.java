package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Person;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonEntity {

    private String homeEppn;
    private String hostEppn;
    private String firstNames;
    private String surName;
    private String givenName;

    public static PersonEntity from(Person source) {
        if (source == null) {
            return null;
        }
        return new PersonEntity(
            source.getHomeEppn(),
            source.getHostEppn(),
            source.getFirstNames(),
            source.getSurName(),
            source.getGivenName()
        );
    }

    public static List<PersonEntity> from(List<Person> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(PersonEntity::from).toList();
    }
}
