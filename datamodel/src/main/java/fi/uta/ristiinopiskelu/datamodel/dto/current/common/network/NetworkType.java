package fi.uta.ristiinopiskelu.datamodel.dto.current.common.network;

import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkTypeEntity;

import java.util.Arrays;

public enum NetworkType {

    CURRICULUM_LEVEL,
    FREEDOM_OF_CHOICE;

    public static NetworkType from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkType networkType) {
        if(networkType == null) {
            return null;
        }

        return Arrays.stream(NetworkType.values())
            .filter(nt -> nt.name().equals(networkType.name()))
            .findFirst()
            .orElse(null);
    }

    public static NetworkType from(NetworkTypeEntity networkType) {
        if(networkType == null) {
            return null;
        }

        return Arrays.stream(NetworkType.values())
            .filter(nt -> nt.name().equals(networkType.name()))
            .findFirst()
            .orElse(null);
    }
}
