package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.CreateRegistrationSelectionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationReplySelectionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
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
public class CreateRegistrationSelection extends AbstractRegistrationSelection implements ParentAwareRegistrationSelection, StatusAwareRegistrationSelection {

    @Field(type = FieldType.Integer)
    private RegistrationSelectionItemStatus selectionItemStatus;

    private String selectionItemStatusInfo;
    private List<String> subGroupSelections;

    // hidden for now, otherwise it will cause a stackoverflow due to an endless loop in springwolf
    @Schema(hidden = true)
    private CreateRegistrationSelectionParent parent;

    @Builder
    public CreateRegistrationSelection(String selectionItemId, RegistrationSelectionItemType selectionItemType,
                                       RegistrationSelectionItemStatus selectionItemStatus, String selectionItemStatusInfo,
                                       List<String> subGroupSelections, CreateRegistrationSelectionParent parent) {
        super(selectionItemId, selectionItemType);
        this.selectionItemStatus = selectionItemStatus;
        this.selectionItemStatusInfo = selectionItemStatusInfo;
        this.subGroupSelections = subGroupSelections;
        this.parent = parent;
    }

    public static CreateRegistrationSelection from(CreateRegistrationSelectionEntity source) {
        if (source == null) {
            return null;
        }

        return CreateRegistrationSelection.builder()
            .selectionItemId(source.getSelectionItemId())
            .selectionItemType(RegistrationSelectionItemType.from(source.getSelectionItemType()))
            .selectionItemStatus(RegistrationSelectionItemStatus.from(source.getSelectionItemStatus()))
            .selectionItemStatusInfo(source.getSelectionItemStatusInfo())
            .subGroupSelections(source.getSubGroupSelections())
            .parent(CreateRegistrationSelectionParent.from(source.getParent()))
            .build();
    }

    public static CreateRegistrationSelection fromReply(RegistrationReplySelection reply) {
        if (reply == null) {
            return null;
        }

        return CreateRegistrationSelection.builder()
            .selectionItemId(reply.getSelectionItemId())
            .selectionItemType(reply.getSelectionItemType())
            .selectionItemStatus(reply.getSelectionItemStatus())
            .selectionItemStatusInfo(reply.getSelectionItemStatusInfo())
            .subGroupSelections(reply.getSubGroupSelections())
            .build();
    }

    public static List<CreateRegistrationSelection> fromReply(List<RegistrationReplySelection> selectionsReplies) {
        if(CollectionUtils.isEmpty(selectionsReplies)) {
            return Collections.emptyList();
        }

        return selectionsReplies.stream()
            .map(CreateRegistrationSelection::fromReply)
            .toList();
    }

    public static CreateRegistrationSelection fromReplyEntity(RegistrationReplySelectionEntity reply) {
        if (reply == null) {
            return null;
        }

        return CreateRegistrationSelection.builder()
            .selectionItemId(reply.getSelectionItemId())
            .selectionItemType(RegistrationSelectionItemType.from(reply.getSelectionItemType()))
            .selectionItemStatus(RegistrationSelectionItemStatus.from(reply.getSelectionItemStatus()))
            .selectionItemStatusInfo(reply.getSelectionItemStatusInfo())
            .subGroupSelections(reply.getSubGroupSelections())
            .build();
    }

    public static List<CreateRegistrationSelection> fromReplyEntity(List<RegistrationReplySelectionEntity> selectionsReplies) {
        if(CollectionUtils.isEmpty(selectionsReplies)) {
            return Collections.emptyList();
        }

        return selectionsReplies.stream()
            .map(CreateRegistrationSelection::fromReplyEntity)
            .toList();
    }
}
