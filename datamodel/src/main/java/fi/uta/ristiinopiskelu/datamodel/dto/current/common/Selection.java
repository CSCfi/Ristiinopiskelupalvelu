package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import java.util.List;

/**
 * Selection for course unit
 *
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * <p>
 * Based on
 * https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class Selection {

    private LocalisedString title;
    private SelectionType type;
    private List<SelectionValue> selectionValues;

    public LocalisedString getTitle() {
        return title;
    }

    public void setTitle(LocalisedString title) {
        this.title = title;
    }

    public SelectionType getType() {
        return type;
    }

    public void setType(SelectionType type) {
        this.type = type;
    }

    public List<SelectionValue> getSelectionValues() {
        return selectionValues;
    }

    public void setSelectionValues(List<SelectionValue> selectionValues) {
        this.selectionValues = selectionValues;
    }
}
