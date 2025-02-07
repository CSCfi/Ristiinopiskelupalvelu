package fi.uta.ristiinopiskelu.handler.integration.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.persistence.repository.DeadLetterQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class DeadLetterQueueRepositoryIntegrationTest {

    @Autowired
    private DeadLetterQueueRepository deadLetterQueueRepository;

    @Test
    public void testFindByOrganisationIdAndEmailSentOrderByConsumedTimestampAsc_streamPast10kLimit_shouldSucceed() {
        generateDeadLetterQueueEntities("12345", MessageType.COURSEUNIT_CREATED_NOTIFICATION, 11000);

        PageRequest pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Order.asc("consumedTimestamp")));

        List<DeadLetterQueueEntity> deadLetters;

        try(Stream<DeadLetterQueueEntity> stream = deadLetterQueueRepository.findByOrganisationIdAndEmailSent("12345", false, pageRequest)) {
            deadLetters = new ArrayList<>(stream.toList());
        }

        assertEquals(11000, deadLetters.size());

        deadLetters.forEach(dl -> {
            dl.setEmailSent(true);
            dl.setVersion(dl.getVersion() + 1);
        });
        deadLetterQueueRepository.saveAll(deadLetters);

        try(Stream<DeadLetterQueueEntity> stream = deadLetterQueueRepository.findByOrganisationIdAndEmailSent("12345", false, pageRequest)) {
            deadLetters = new ArrayList<>(stream.toList());
        }

        assertEquals(0, deadLetters.size());
    }

    private List<DeadLetterQueueEntity> generateDeadLetterQueueEntities(String organisationId, MessageType messageType, int amount) {
        List<DeadLetterQueueEntity> entities = new ArrayList<>();

        for(int i = 0; i < amount; i++) {
            entities.add(DeadLetterQueueEntity.builder()
                .id(UUID.randomUUID().toString())
                .version(0L)
                .organisationId(organisationId)
                .emailSent(false)
                .consumedTimestamp(OffsetDateTime.now())
                .messageType(messageType.name())
                .message("{ some: \"json\" }")
                .build());
        }

        return StreamSupport.stream(deadLetterQueueRepository.saveAll(entities).spliterator(), false)
            .toList();
    }
}
