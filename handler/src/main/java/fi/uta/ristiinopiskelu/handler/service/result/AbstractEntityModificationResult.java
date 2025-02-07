package fi.uta.ristiinopiskelu.handler.service.result;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import org.springframework.util.Assert;

public abstract class AbstractEntityModificationResult<T extends GenericEntity> implements EntityModificationResult<T> {

    private enum Direction {
        PREVIOUS,
        CURRENT
    }

    private final ModificationOperationType operationType;
    private T previous;
    private T current;

    private void validate(Direction direction, ModificationOperationType operationType, T value) {
        if(operationType == ModificationOperationType.CREATE) {
            if(direction == Direction.PREVIOUS) {
                Assert.isNull(value, "previous entity must be null if modification operation is of type CREATE");
            } else {
                Assert.notNull(value, "current entity cannot be null if modification operation is of type CREATE");
            }
        } else if(operationType == ModificationOperationType.UPDATE) {
            if(direction == Direction.PREVIOUS) {
                Assert.notNull(value, "previous entity cannot be null if modification operation is of type UPDATE");
            } else {
                Assert.notNull(value, "current entity cannot be null if modification operation is of type UPDATE");
            }
        } else if(operationType == ModificationOperationType.DELETE) {
            if(direction == Direction.PREVIOUS) {
                Assert.notNull(value, "previous entity cannot be null if modification operation is of type DELETE");
            } else {
                Assert.isNull(value, "current entity must be null if modification operation is of type DELETE");
            }
        } else {
            throw new IllegalArgumentException("Unknown operation type %s".formatted(operationType));
        }
    }

    public AbstractEntityModificationResult(ModificationOperationType operationType, T previous, T current) {
        validate(Direction.PREVIOUS, operationType, previous);
        validate(Direction.CURRENT, operationType, current);
        this.operationType = operationType;
        this.previous = previous;
        this.current = current;
    }

    public ModificationOperationType getOperationType() {
        return operationType;
    }
    
    public T getPrevious() {
        return previous;
    }

    public void setPrevious(T previous) {
        validate(Direction.PREVIOUS, operationType, previous);
        this.previous = previous;
    }

    public T getCurrent() {
        return current;
    }

    public void setCurrent(T current) {
        validate(Direction.CURRENT, operationType, current);
        this.current = current;
    }
}
