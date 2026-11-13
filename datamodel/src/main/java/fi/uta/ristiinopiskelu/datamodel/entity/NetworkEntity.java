package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ExpenseEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ValidityEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "verkostot", createIndex = false)
public class NetworkEntity extends GenericEntity implements Serializable {

    private LocalisedStringEntity name;
    private String abbreviation;
    private List<NetworkOrganisationEntity> organisations;
    private NetworkTypeEntity networkType;
    private LocalisedStringEntity description;
    private LocalisedStringEntity furtherInformation;
    private boolean published;
    private List<CodeReferenceEntity> targetGroups;
    private List<CodeReferenceEntity> restrictions;
    private ValidityEntity validity;
    private ExpenseEntity expenses;
    private Boolean deleted;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime deletedTimestamp;

    public static NetworkEntity from(NetworkWriteDTO network) {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId(network.getId());
        networkEntity.setName(LocalisedStringEntity.from(network.getName()));
        networkEntity.setAbbreviation(network.getAbbreviation());
        networkEntity.setOrganisations(NetworkOrganisationEntity.from(network.getOrganisations()));
        networkEntity.setNetworkType(NetworkTypeEntity.from(network.getNetworkType()));
        networkEntity.setDescription(LocalisedStringEntity.from(network.getDescription()));
        networkEntity.setFurtherInformation(LocalisedStringEntity.from(network.getFurtherInformation()));
        networkEntity.setValidity(ValidityEntity.from(network.getValidity()));
        networkEntity.setExpenses(ExpenseEntity.from(network.getExpenses()));
        networkEntity.setTargetGroups(CodeReferenceEntity.from(network.getTargetGroups()));
        networkEntity.setRestrictions(CodeReferenceEntity.from(network.getRestrictions()));
        networkEntity.setPublished(network.isPublished());
        return networkEntity;
    }

    public static NetworkEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network source) {
        if (source == null) {
            return null;
        }

        NetworkEntity target = new NetworkEntity();
        target.setId(source.getId());
        target.setName(LocalisedStringEntity.from(source.getName()));
        target.setAbbreviation(source.getAbbreviation());
        target.setNetworkType(NetworkTypeEntity.from(source.getNetworkType()));
        target.setDescription(LocalisedStringEntity.from(source.getDescription()));
        target.setFurtherInformation(LocalisedStringEntity.from(source.getFurtherInformation()));
        target.setValidity(ValidityEntity.from(source.getValidity()));
        target.setOrganisations(NetworkOrganisationEntity.fromV8(source.getOrganisations()));
        target.setExpenses(ExpenseEntity.from(source.getExpenses()));
        target.setPublished(source.getPublished());
        return target;
    }
}
