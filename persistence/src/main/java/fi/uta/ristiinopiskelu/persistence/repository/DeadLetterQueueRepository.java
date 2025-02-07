package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;
import org.springframework.data.domain.Pageable;

import java.util.stream.Stream;

public interface DeadLetterQueueRepository extends ExtendedRepository<DeadLetterQueueEntity, String> {

    Stream<DeadLetterQueueEntity> findByOrganisationIdAndEmailSent(String organisationId, boolean emailSent, Pageable pageable);
}
