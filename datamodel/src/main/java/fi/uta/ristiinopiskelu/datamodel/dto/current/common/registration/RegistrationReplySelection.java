package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class RegistrationReplySelection extends AbstractRegistrationSelection implements StatusAwareRegistrationSelection {

    @Field(type = FieldType.Integer)
    private RegistrationSelectionItemStatus selectionItemStatus;

    private String selectionItemStatusInfo;
    private List<String> subGroupSelections;
    private Rank rank;

    public RegistrationReplySelection(String selectionItemId, RegistrationSelectionItemType selectionItemType,
                                      RegistrationSelectionItemStatus selectionItemStatus, String selectionItemStatusInfo,
                                      List<String> subGroupSelections, Rank rank) {
        super(selectionItemId, selectionItemType);
        this.selectionItemStatus = selectionItemStatus;
        this.selectionItemStatusInfo = selectionItemStatusInfo;
        this.subGroupSelections = subGroupSelections;
        this.rank = rank;
    }

    public static RegistrationReplySelection fromCreate(CreateRegistrationSelection selection) {
        if (selection == null) {
            return null;
        }

        return new RegistrationReplySelection(
            selection.getSelectionItemId(),
            selection.getSelectionItemType(),
            selection.getSelectionItemStatus(),
            selection.getSelectionItemStatusInfo(),
            selection.getSubGroupSelections(),
            null
        );
    }

    public static List<RegistrationReplySelection> fromCreate(List<CreateRegistrationSelection> selections) {
        if (CollectionUtils.isEmpty(selections)) {
            return Collections.emptyList();
        }

        return selections.stream()
            .map(RegistrationReplySelection::fromCreate)
            .toList();
    }
}
