package fi.uta.ristiinopiskelu.datamodel.entity.common.network;

import java.util.Arrays;

public enum NetworkTypeEntity {

    CURRICULUM_LEVEL,
    FREEDOM_OF_CHOICE;

    public static NetworkTypeEntity from(fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkType source) {
        if (source == null) {
            return null;
        }
        return Arrays.stream(NetworkTypeEntity.values())
            .filter(v -> v.name().equals(source.name()))
            .findFirst()
            .orElse(null);
    }

    public static NetworkTypeEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkType source) {
        if (source == null) {
            return null;
        }
        return Arrays.stream(NetworkTypeEntity.values())
            .filter(v -> v.name().equals(source.name()))
            .findFirst()
            .orElse(null);
    }
}
