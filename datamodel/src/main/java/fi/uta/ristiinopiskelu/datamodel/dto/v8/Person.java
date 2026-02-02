package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "PersonV8", title = "PersonV8")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Person {

  private String homeEppn;
  private String hostEppn;
  private String firstNames;
  private String surName;
  private String givenName;
}
