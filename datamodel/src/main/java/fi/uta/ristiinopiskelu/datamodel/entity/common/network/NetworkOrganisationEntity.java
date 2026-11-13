package fi.uta.ristiinopiskelu.datamodel.entity.common.network;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkOrganisationEntity {

    private String organisationTkCode = null;
    private Boolean isCoordinator = null;
    private ValidityEntity validityInNetwork = null;

    public static List<NetworkOrganisationEntity> from(List<NetworkOrganisation> source) {
        if (source == null) {
            return null;
        }

        List<NetworkOrganisationEntity> target = new ArrayList<>();
        for (NetworkOrganisation item : source) {
            if (item == null) {
                target.add(null);
            } else {
                target.add(new NetworkOrganisationEntity(
                    item.getOrganisationTkCode(),
                    item.getIsCoordinator(),
                    ValidityEntity.from(item.getValidityInNetwork())));
            }
        }
        return target;
    }

    public static List<NetworkOrganisationEntity> fromV8(List<fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkOrganisation> source) {
        if (source == null) {
            return null;
        }

        List<NetworkOrganisationEntity> target = new ArrayList<>();
        for (fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkOrganisation item : source) {
            if (item == null) {
                target.add(null);
            } else {
                target.add(new NetworkOrganisationEntity(
                    item.getOrganisationTkCode(),
                    item.getIsCoordinator(),
                    ValidityEntity.from(item.getValidityInNetwork())));
            }
        }
        return target;
    }
}
