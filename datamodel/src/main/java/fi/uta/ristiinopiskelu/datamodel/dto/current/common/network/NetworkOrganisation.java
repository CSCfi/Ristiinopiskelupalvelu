package fi.uta.ristiinopiskelu.datamodel.dto.current.common.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

@Schema(name = "NetworkOrganisationV9", title = "NetworkOrganisationV9")
public class NetworkOrganisation {
    @JsonProperty("organisationTkCode")
    private String organisationTkCode = null;

    @JsonProperty("isCoordinator")
    private Boolean isCoordinator = null;

    @JsonProperty("validityInNetwork")
    private Validity validityInNetwork = null;

    public NetworkOrganisation() {

    }

    public NetworkOrganisation(String organisationTkCode, Boolean isCoordinator, Validity validityInNetwork) {
        this.organisationTkCode = organisationTkCode;
        this.isCoordinator = isCoordinator;
        this.validityInNetwork = validityInNetwork;
    }

    public String getOrganisationTkCode() {
        return organisationTkCode;
    }

    public void setOrganisationTkCode(String organisationTkCode) {
        this.organisationTkCode = organisationTkCode;
    }

    public Boolean getIsCoordinator() {
        return isCoordinator;
    }

    public void setIsCoordinator(Boolean isCoordinator) {
        this.isCoordinator = isCoordinator;
    }

    public Validity getValidityInNetwork() {
        return validityInNetwork;
    }

    public void setValidityInNetwork(Validity validityInNetwork) {
        this.validityInNetwork = validityInNetwork;
    }

    public static List<NetworkOrganisation> fromV8(List<fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkOrganisation> source) {
        if (source == null) {
            return List.of();
        }

        return source.stream().map(org -> {
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation target =
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation();
            target.setOrganisationTkCode(org.getOrganisationTkCode());
            target.setIsCoordinator(org.getIsCoordinator());
            target.setValidityInNetwork(Validity.from(org.getValidityInNetwork()));
            return target;
        }).collect(Collectors.toList());
    }

    public static List<NetworkOrganisation> from(List<NetworkOrganisationEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream()
            .map(NetworkOrganisation::from)
            .toList();
    }

    public static NetworkOrganisation from(NetworkOrganisationEntity source) {
        if (source == null) {
            return null;
        }
        NetworkOrganisation target = new NetworkOrganisation();
        target.setOrganisationTkCode(source.getOrganisationTkCode());
        target.setIsCoordinator(source.getIsCoordinator());
        target.setValidityInNetwork(Validity.from(source.getValidityInNetwork()));
        return target;
    }
}
