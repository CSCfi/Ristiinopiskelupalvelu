package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Address
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class Address {

  @JsonProperty("street")
  private String street = null;

  @JsonProperty("postalCode")
  private String postalCode = null;

  @JsonProperty("postOffice")
  private String postOffice = null;
  
  @JsonProperty("country")
  private Country country = null;

  /**
   * M2 4.1.5/6.1
   * 
   * @return street
   **/

  @Schema(description = "M2 4.1.5/6.1")
  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public Address postalCode(String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  /**
   * In Finland they are numbers < 100 000
   * 
   * @return postalCode
   **/
  @Schema(description = "In Finland they are numbers < 100 000")
  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  /**
   * M2 4.1.5/6.3
   * 
   * @return postOffice
   **/

  @Schema(description = "M2 4.1.5/6.3")
  public String getPostOffice() {
    return postOffice;
  }

  public void setPostOffice(String postOffice) {
    this.postOffice = postOffice;
  }

  /**
   * M2 4.1.5/6.4
   * 
   * @return country
   **/
  @Schema(description = "M2 4.1.5/6.4")
  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }
}
