package fi.uta.ristiinopiskelu.messaging.message.current.notification;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractNotification;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompositeIdentifiedEntityModifiedNotification extends AbstractNotification {

    private List<CompositeIdentifiedEntityReference> created = new ArrayList<>();
    private List<CompositeIdentifiedEntityReference> updated = new ArrayList<>();
    private List<CompositeIdentifiedEntityReference> deleted = new ArrayList<>();

    public CompositeIdentifiedEntityModifiedNotification() {
    }

    public CompositeIdentifiedEntityModifiedNotification(String sendingOrganisationTkCode, OffsetDateTime timestamp,
                                                         List<CompositeIdentifiedEntityReference> created,
                                                         List<CompositeIdentifiedEntityReference> updated,
                                                         List<CompositeIdentifiedEntityReference> deleted) {
        super(sendingOrganisationTkCode, timestamp);

        if(!CollectionUtils.isEmpty(created)) {
            this.created = created;
        }

        if(!CollectionUtils.isEmpty(updated)) {
            this.updated = updated;
        }

        if(!CollectionUtils.isEmpty(deleted)) {
            this.deleted = deleted;
        }
    }

    public List<CompositeIdentifiedEntityReference> getCreated() {
        return created;
    }

    public List<CompositeIdentifiedEntityReference> getUpdated() {
        return updated;
    }

    public List<CompositeIdentifiedEntityReference> getDeleted() {
        return deleted;
    }

    public void setCreated(List<CompositeIdentifiedEntityReference> created) {
        this.created = created;
    }

    public void setUpdated(List<CompositeIdentifiedEntityReference> updated) {
        this.updated = updated;
    }

    public void setDeleted(List<CompositeIdentifiedEntityReference> deleted) {
        this.deleted = deleted;
    }

    public List<CompositeIdentifiedEntityReference> getCreated(CompositeIdentifiedEntityType type) {
        return this.getCreated().stream().filter(ref -> ref.getType() == type).collect(Collectors.toList());
    }

    public List<CompositeIdentifiedEntityReference> getUpdated(CompositeIdentifiedEntityType type) {
        return this.getUpdated().stream().filter(ref -> ref.getType() == type).collect(Collectors.toList());
    }

    public List<CompositeIdentifiedEntityReference> getDeleted(CompositeIdentifiedEntityType type) {
        return this.getDeleted().stream().filter(ref -> ref.getType() == type).collect(Collectors.toList());
    }
}
