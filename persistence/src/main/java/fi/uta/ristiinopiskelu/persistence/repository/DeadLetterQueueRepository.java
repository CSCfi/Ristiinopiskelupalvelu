package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;

import java.util.List;

public interface DeadLetterQueueRepository extends ExtendedRepository<DeadLetterQueueEntity, String> {
    List<DeadLetterQueueEntity> findAllByOrganisationIdAndEmailSentOrderByConsumedTimestampAsc(String organisationId, boolean emailSent);
}
