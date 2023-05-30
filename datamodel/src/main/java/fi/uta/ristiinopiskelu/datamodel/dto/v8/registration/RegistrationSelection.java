package fi.uta.ristiinopiskelu.datamodel.dto.v8.registration;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

public class RegistrationSelection {
    private String selectionItemId;
    private RegistrationSelectionItemType selectionItemType;

    @Field(type = FieldType.Integer)
    private RegistrationSelectionItemStatus selectionItemStatus;

    private String selectionItemStatusInfo;
    private RegistrationSelection parent;
    private List<String> subGroupSelections;
    private Rank rank;

    public Rank getRank() { return rank; }

    public void setRank(Rank rank) {this.rank = rank; }

    public RegistrationSelectionItemStatus getSelectionItemStatus() {
        return selectionItemStatus;
    }

    public void setSelectionItemStatus(RegistrationSelectionItemStatus selectionItemStatus) {
        this.selectionItemStatus = selectionItemStatus;
    }

    public String getSelectionItemStatusInfo() {
        return selectionItemStatusInfo;
    }

    public void setSelectionItemStatusInfo(String selectionItemStatusInfo) {
        this.selectionItemStatusInfo = selectionItemStatusInfo;
    }

    public String getSelectionItemId() {
        return selectionItemId;
    }

    public void setSelectionItemId(String selectionItemId) {
        this.selectionItemId = selectionItemId;
    }

    public RegistrationSelectionItemType getSelectionItemType() {
        return selectionItemType;
    }

    public void setSelectionItemType(RegistrationSelectionItemType selectionItemType) {
        this.selectionItemType = selectionItemType;
    }

    public RegistrationSelection getParent() {
        return parent;
    }

    public void setParent(RegistrationSelection parent) {
        this.parent = parent;
    }

    public List<String> getSubGroupSelections() {
        return subGroupSelections;
    }

    public void setSubGroupSelections(List<String> subGroupSelections) {
        this.subGroupSelections = subGroupSelections;
    }
}
