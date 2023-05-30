package fi.uta.ristiinopiskelu.dlqhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.dlqhandler.component.Attachment;
import fi.uta.ristiinopiskelu.dlqhandler.component.EmailSender;
import fi.uta.ristiinopiskelu.persistence.repository.DeadLetterQueueRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.activation.DataSource;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class EmailSenderTest {
    private EmailSender emailSender;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private OrganisationRepository organisationRepository;

    @MockBean
    private DeadLetterQueueRepository deadLetterQueueRepository;

    private ObjectMapper objectMapper;

    private final String administratorEmail = "testi@test.com";
    private final String noreplyEmail = "testi-noreply@test.com";
    private final String subject = "Test subject";
    private final String text = "Test body";

    private List<OrganisationEntity> organisationEntitiesForTest;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper();
        emailSender = new EmailSender(organisationRepository, deadLetterQueueRepository, javaMailSender, objectMapper, administratorEmail, noreplyEmail, true, subject, text);

        organisationEntitiesForTest = new ArrayList<>();
        OrganisationEntity organisationEntity = new OrganisationEntity();
        organisationEntity.setId("ORG1");
        organisationEntity.setAdministratorEmail("testiorg@testi.com");
        organisationEntity.setOrganisationName(new LocalisedString("TestiOrganisaatio 1", null, null));
        organisationEntity.setQueue("org1-jono");

        OrganisationEntity organisationEntity2 = new OrganisationEntity();
        organisationEntity2.setId("ORG2");
        organisationEntity2.setAdministratorEmail("testiorg2@testi.com");
        organisationEntity2.setOrganisationName(new LocalisedString("TestiOrganisaatio 2", null, null));
        organisationEntity2.setQueue("org2-jono");

        organisationEntitiesForTest.add(organisationEntity);
        organisationEntitiesForTest.add(organisationEntity2);
    }

    @Test
    public void emailSendingForOneOrganisationAndTwoAttachments_shouldSuccess() throws Exception {
        Iterable<OrganisationEntity> organisationEntityIterable = organisationEntitiesForTest;
        when(organisationRepository.findAll()).thenReturn(organisationEntityIterable);

        String messageType1 = "UNKNOWN_MESSAGE";
        String messageType2 = "CREATE_REGISTRATION_REQUEST";

        DeadLetterQueueEntity deadLetterQueueEntity = new DeadLetterQueueEntity();
        deadLetterQueueEntity.setEmailSent(false);
        deadLetterQueueEntity.setOrganisationId(organisationEntitiesForTest.get(0).getId());
        deadLetterQueueEntity.setMessageType(messageType1);
        deadLetterQueueEntity.setMessage("Testiviesti 1");

        DeadLetterQueueEntity deadLetterQueueEntity2 = new DeadLetterQueueEntity();
        deadLetterQueueEntity2.setEmailSent(false);
        deadLetterQueueEntity2.setOrganisationId(organisationEntitiesForTest.get(0).getId());
        deadLetterQueueEntity2.setMessageType(messageType1);
        deadLetterQueueEntity2.setMessage("Testiviesti 2");

        DeadLetterQueueEntity deadLetterQueueEntity3 = new DeadLetterQueueEntity();
        deadLetterQueueEntity3.setEmailSent(false);
        deadLetterQueueEntity3.setOrganisationId(organisationEntitiesForTest.get(0).getId());
        deadLetterQueueEntity3.setMessageType(messageType2);
        deadLetterQueueEntity3.setMessage("Testiviesti 3");

        List<DeadLetterQueueEntity> deadLetterQueueEntities = Arrays.asList(deadLetterQueueEntity, deadLetterQueueEntity2, deadLetterQueueEntity3);

        when(deadLetterQueueRepository.findAllByOrganisationIdAndEmailSentOrderByConsumedTimestampAsc(
                eq(organisationEntitiesForTest.get(0).getId()), anyBoolean())).thenReturn(deadLetterQueueEntities);
        when(deadLetterQueueRepository.findAllByOrganisationIdAndEmailSentOrderByConsumedTimestampAsc(
                eq(organisationEntitiesForTest.get(1).getId()), anyBoolean())).thenReturn(null);

        MimeMessage message = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        doNothing().when(javaMailSender).send(message);
        when(deadLetterQueueRepository.update(any())).thenReturn(null);
        emailSender.sendDeadLetterQueueEmail();

        Assertions.assertEquals(subject, message.getSubject());

        MimeMessageParser parser = new MimeMessageParser(message);
        parser.parse();

        Assertions.assertEquals(parser.getAttachmentList().size(), deadLetterQueueEntities.stream().map(dl -> dl.getMessageType()).distinct().collect(Collectors.toList()).size());
        Assertions.assertTrue(parser.getAttachmentList().stream().anyMatch(a -> a.getName().equalsIgnoreCase(messageType1 + ".json")));
        Assertions.assertTrue(parser.getAttachmentList().stream().anyMatch(a -> a.getName().equalsIgnoreCase(messageType2 + ".json")));

        for(DataSource attachmentDatasource : parser.getAttachmentList()) {
            Attachment attachment = objectMapper.readValue(attachmentDatasource.getInputStream(), Attachment.class);
            Assertions.assertTrue(deadLetterQueueEntities.stream().anyMatch(dl -> dl.getMessageType().equals(attachment.getMessageType())));
            Assertions.assertEquals(
                    deadLetterQueueEntities.stream().filter(dl -> dl.getMessageType().equals(attachment.getMessageType())).map(dl -> dl.getMessage()).collect(Collectors.toList()),
                    attachment.getMessages());
        }

        verify(deadLetterQueueRepository, times(2)).findAllByOrganisationIdAndEmailSentOrderByConsumedTimestampAsc(anyString(), anyBoolean());
        verify(javaMailSender, times(1)).send(message);
        verify(deadLetterQueueRepository, times(deadLetterQueueEntities.size())).update(any());
    }
}
