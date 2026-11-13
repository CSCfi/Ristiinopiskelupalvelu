package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.SelectionTypeEntity;

public enum SelectionType {
    CHOOSE_ONE,
    CHOOSE_MANY;

    public static SelectionType from(SelectionTypeEntity source) {
        return source == null ? null : SelectionType.valueOf(source.name());
    }
}
