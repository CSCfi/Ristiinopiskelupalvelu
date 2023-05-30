package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

/**
 * CooperationNetwork
 *
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * <p>
 * Based on
 * https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class CooperationNetwork {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("name")
    private LocalisedString name = null;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityStartDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityEndDate;

    private Boolean enrollable;

    /**
     * URI M2 1.1.5.1
     *
     * @return id
     **/
    @Schema(description = "URI M2 1.1.5.1", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * M2 1.1.5.2
     *
     * @return name
     **/
    @Schema(description = "M2 1.1.5.2", required = true)
    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public LocalDate getValidityStartDate() {
        return validityStartDate;
    }

    public void setValidityStartDate(LocalDate validityStartDate) {
        this.validityStartDate = validityStartDate;
    }

    public LocalDate getValidityEndDate() {
        return validityEndDate;
    }

    public void setValidityEndDate(LocalDate validityEndDate) {
        this.validityEndDate = validityEndDate;
    }

    public Boolean getEnrollable() {
        return enrollable;
    }

    public void setEnrollable(Boolean enrollable) {
        this.enrollable = enrollable;
    }
}
