package fi.uta.ristiinopiskelu.handler.service.result;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class DefaultCompositeIdentifiedEntityModificationResult implements CompositeIdentifiedEntityModificationResult {

    private List<? extends CompositeIdentifiedEntity> created = new ArrayList<>();
    private List<? extends CompositeIdentifiedEntity> updated = new ArrayList<>();
    private List<? extends CompositeIdentifiedEntity> deleted = new ArrayList<>();

    public DefaultCompositeIdentifiedEntityModificationResult() {

    }

    public DefaultCompositeIdentifiedEntityModificationResult(List<? extends CompositeIdentifiedEntity> created, List<? extends CompositeIdentifiedEntity> updated) {
        if(!CollectionUtils.isEmpty(created)) {
            this.created = created;
        }

        if(!CollectionUtils.isEmpty(updated)) {
            this.updated = updated;
        }
    }

    public DefaultCompositeIdentifiedEntityModificationResult(List<? extends CompositeIdentifiedEntity> created,
                                                              List<? extends CompositeIdentifiedEntity> updated,
                                                              List<? extends CompositeIdentifiedEntity> deleted) {
        this(created, updated);
        if(!CollectionUtils.isEmpty(deleted)) {
            this.deleted = deleted;
        }
    }

    @Override
    public List<? extends CompositeIdentifiedEntity> getCreated() {
        return created;
    }

    @Override
    public List<? extends CompositeIdentifiedEntity> getUpdated() {
        return updated;
    }

    @Override
    public List<? extends CompositeIdentifiedEntity> getDeleted() {
        return deleted;
    }

    @Override
    public long getCreatedAmount(CompositeIdentifiedEntityType type) {
        return created.stream().filter(entity -> entity.getType() == type).count();
    }

    @Override
    public long getUpdatedAmount(CompositeIdentifiedEntityType type) {
        return updated.stream().filter(entity -> entity.getType() == type).count();
    }
}
