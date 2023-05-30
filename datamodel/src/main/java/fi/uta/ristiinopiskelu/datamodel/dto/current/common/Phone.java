package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Phone
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi> 
 *         Based on https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class Phone   {

  @JsonProperty("number")
  private String number = null;
  
  @JsonProperty("description")
  private String description = null;
  
  public Phone number(String number) {
    this.number = number;
    return this;
  }
  
  /**
   * M2 4.1.7.1
   * @return number
  **/
  @Schema(required = true, description = "M2 4.1.7.1")
  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  /**
   * M2 4.1.7.2 \"e.g. secretariat\"
   * @return description
  **/
 
  @Schema(description = "M2 4.1.7.2 \"e.g. secretariat\"")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

}




