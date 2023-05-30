package fi.uta.ristiinopiskelu.datamodel.dto.v8.network;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.validation.CodeSetConstraint;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import java.util.List;

public class Network {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("name")
    private LocalisedString name = null;

    @JsonProperty("abbreviation")
    private String abbreviation = null;

    @JsonProperty("networkType")
    private NetworkType networkType = null;

    @JsonProperty("description")
    private LocalisedString description = null;

    @Valid
    @JsonProperty("targetGroups")
    private List<@CodeSetConstraint(codeSetKey = "study_right_type") CodeReference> targetGroups = null;

    @JsonProperty("organisations")
    private List<NetworkOrganisation> organisations = null;

    @JsonProperty("furtherInformation")
    private LocalisedString furtherInformation = null;

    @Valid
    @JsonProperty("restrictions")
    private List<@CodeSetConstraint(codeSetKey = "study_right_type") CodeReference> restrictions = null;

    @JsonProperty("validity")
    private Validity validity = null;

    @JsonProperty("expenses")
    private Expense expenses = null;

    @JsonProperty("published")
    private Boolean published;

    @Schema(description = "URI M2 1.1.5.1", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * M2 1.1.5.2
     * @return name
     **/
    @Schema(description = "M2 1.1.5.2", required = true)
    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    @JsonIgnore
    public List<NetworkOrganisation> getOrganisations() {
        return organisations;
    }

    @JsonProperty("organisations")
    public void setOrganisations(List<NetworkOrganisation> organisations) {
        this.organisations = organisations;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }

    public LocalisedString getDescription() {
        return description;
    }

    public void setDescription(LocalisedString description) {
        this.description = description;
    }

    public List<CodeReference> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(List<CodeReference> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public LocalisedString getFurtherInformation() {
        return furtherInformation;
    }

    public void setFurtherInformation(LocalisedString furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    public List<CodeReference> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<CodeReference> restrictions) {
        this.restrictions = restrictions;
    }

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public Expense getExpenses() {
        return expenses;
    }

    public void setExpenses(Expense expenses) {
        this.expenses = expenses;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }
}
