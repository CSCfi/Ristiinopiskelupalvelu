package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeReference;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Personreference
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class PersonReference {

  /**
   * M2 table 7 1.1
   * 
   * - Teacher
   * 
   * - Corresponding teacher
   * 
   * - Tutor
   * 
   * - Student
   * 
   * - Support person
   * 
   * - Contact person
   * 
   * - Other person
   */
  @JsonProperty("personRole")
  private CodeReference personRole;

  @JsonProperty("person")
  private Person person = null;

  @JsonProperty("definition")
  private String definition = null;

  /**
   * M2 table 7 1.3
   * 
   * @return definition
   **/
  @Schema(description = "M2 table 7 1.3")
  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public CodeReference getPersonRole() {
    return personRole;
  }

  public void setPersonRole(CodeReference personRole) {
    this.personRole = personRole;
  }

  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }
}
