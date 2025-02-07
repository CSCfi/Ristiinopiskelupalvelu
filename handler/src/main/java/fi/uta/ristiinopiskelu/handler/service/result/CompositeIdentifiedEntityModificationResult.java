package fi.uta.ristiinopiskelu.handler.service.result;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import org.springframework.util.Assert;

public class CompositeIdentifiedEntityModificationResult extends AbstractEntityModificationResult<CompositeIdentifiedEntity> {

    private CompositeIdentifiedEntityType type;
    
    public CompositeIdentifiedEntityModificationResult(ModificationOperationType operationType, CompositeIdentifiedEntityType type,
                                                       CompositeIdentifiedEntity previous, CompositeIdentifiedEntity current) {
        super(operationType, previous, current);
        Assert.notNull(type, "type must not be null");
        this.type = type;
    }

    public CompositeIdentifiedEntityType getType() {
        return type;
    }

    public void setType(CompositeIdentifiedEntityType type) {
        this.type = type;
    }
}
