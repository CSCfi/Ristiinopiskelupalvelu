package fi.uta.ristiinopiskelu.handler.service.result;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public class GenericEntityModificationResult extends AbstractEntityModificationResult<GenericEntity> {

    public GenericEntityModificationResult(ModificationOperationType operationType, GenericEntity previous, GenericEntity current) {
        super(operationType, previous, current);
    }
}
