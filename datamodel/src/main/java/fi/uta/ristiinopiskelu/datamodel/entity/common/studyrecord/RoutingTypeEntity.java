package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.RoutingType;

public enum RoutingTypeEntity {

    CROSS_STUDY,
    OTHER;

    public static RoutingTypeEntity from(RoutingType source) {
        return source == null ? null : RoutingTypeEntity.valueOf(source.name());
    }
}
