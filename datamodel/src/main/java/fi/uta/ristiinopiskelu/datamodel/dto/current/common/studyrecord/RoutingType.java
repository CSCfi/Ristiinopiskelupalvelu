package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.RoutingTypeEntity;

public enum RoutingType {

    CROSS_STUDY,
    OTHER;

    public static RoutingType from(RoutingTypeEntity source) {
        return source == null ? null : RoutingType.valueOf(source.name());
    }
}
