package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Expense;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = "verkostot", createIndex = false)
public class NetworkEntity extends GenericEntity implements Serializable {
    private LocalisedString name;
    private String abbreviation;
    private List<NetworkOrganisation> organisations;
    private NetworkType networkType;
    private LocalisedString description;
    private LocalisedString furtherInformation;
    private boolean published;
    private List<CodeReference> targetGroups;
    private List<CodeReference> restrictions;
    private Validity validity;
    private Expense expenses;
    private Boolean deleted;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime deletedTimestamp;

    public NetworkEntity() {
    }

    public static NetworkEntity fromDto(NetworkWriteDTO network) {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId(network.getId());
        networkEntity.name = network.getName();
        networkEntity.abbreviation = network.getAbbreviation();
        networkEntity.organisations = network.getOrganisations();
        networkEntity.networkType = network.getNetworkType();
        networkEntity.description = network.getDescription();
        networkEntity.furtherInformation = network.getFurtherInformation();
        networkEntity.validity = network.getValidity();
        networkEntity.expenses = network.getExpenses();
        networkEntity.targetGroups = network.getTargetGroups();
        networkEntity.restrictions = network.getRestrictions();
        networkEntity.published = network.isPublished();
        return networkEntity;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public LocalisedString getName() {
        return this.name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public List<NetworkOrganisation> getOrganisations() {
        return organisations;
    }

    public void setOrganisations(List<NetworkOrganisation> organisations) {
        this.organisations = organisations;
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

    public LocalisedString getFurtherInformation() {
        return furtherInformation;
    }

    public void setFurtherInformation(LocalisedString furtherInformation) {
        this.furtherInformation = furtherInformation;
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

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean status) {
        this.published = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public OffsetDateTime getDeletedTimestamp() {
        return deletedTimestamp;
    }

    public void setDeletedTimestamp(OffsetDateTime deletedTimestamp) {
        this.deletedTimestamp = deletedTimestamp;
    }

    public List<CodeReference> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(List<CodeReference> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public List<CodeReference> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<CodeReference> restrictions) {
        this.restrictions = restrictions;
    }

    public String toString() {
        return "Network(name=" + this.getName() + ")";
    }
}
