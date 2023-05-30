package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

public enum StudiesSearchSortField {

    NONE(null),
    ID("studyElementId"),
    IDENTIFIER_CODE("studyElementIdentifierCode"),
    NAME("name"),
    CREDITS_MIN("creditsMin"),
    CREDITS_MAX("creditsMax");

    private final String fieldName;

    StudiesSearchSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }
}
