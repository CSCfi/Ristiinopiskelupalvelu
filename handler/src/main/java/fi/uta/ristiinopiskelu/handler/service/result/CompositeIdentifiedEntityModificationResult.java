package fi.uta.ristiinopiskelu.handler.service.result;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;

import java.util.List;

public interface CompositeIdentifiedEntityModificationResult {

    List<? extends CompositeIdentifiedEntity> getCreated();

    List<? extends CompositeIdentifiedEntity> getUpdated();

    List<? extends CompositeIdentifiedEntity> getDeleted();

    long getCreatedAmount(CompositeIdentifiedEntityType type);

    long getUpdatedAmount(CompositeIdentifiedEntityType type);
}
