package fi.uta.ristiinopiskelu.handler.service.result;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public interface EntityModificationResult<T extends GenericEntity> {

    ModificationOperationType getOperationType();

    T getPrevious();

    T getCurrent();
}
