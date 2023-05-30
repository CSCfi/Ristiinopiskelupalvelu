package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * Organisationreference
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class OrganisationReference {

  private BigDecimal percent;

  /**
   * M2 table 8 1.1
   * 
   * - mainly responsible
   * 
   * - organiser responsible for curricula
   * 
   * - Implementing
   * 
   * - approving
   * 
   * - coordinating cooperation
   * 
   * - other
   */
  @Field(type = FieldType.Integer)
  private OrganisationRole organisationRole;

  @Field(name = "target")
  @JsonProperty("target")
  private Organisation organisation = null;

  @JsonProperty("description")
  private LocalisedString description = null;

  /**
   * M2 table 8 1.3
   * 
   * @return description
   **/
  @Schema(description = "M2 table 8 1.3")
  public LocalisedString getDescription() {
    return description;
  }

  public void setDescription(LocalisedString description) {
    this.description = description;
  }

  public BigDecimal getPercent() {
    return percent;
  }

  public void setPercent(BigDecimal percent) {
    this.percent = percent;
  }

  public OrganisationRole getOrganisationRole() {
    return organisationRole;
  }

  public void setOrganisationRole(OrganisationRole organisationRole) {
    this.organisationRole = organisationRole;
  }

  @Schema(required = true)
  public Organisation getOrganisation() {
    return organisation;
  }

  public void setOrganisation(Organisation organisation) {
    this.organisation = organisation;
  }
}
