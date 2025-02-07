package fi.uta.ristiinopiskelu.dlqhandler.component;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.DeadLetterQueueRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class EmailSenderTest {

    private static final Logger logger = LoggerFactory.getLogger(EmailSenderTest.class);

    @RegisterExtension
    private static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private EmailSender emailSender;

    @MockBean
    private OrganisationRepository organisationRepository;

    @MockBean
    private DeadLetterQueueRepository deadLetterQueueRepository;

    private final String administratorEmail = "admin@test.com";
    private final String noreplyEmail = "testi-noreply@test.com";
    private final String subject = "Test subject";
    private final String text = """
    Test body. 
    
    Environment: %s
        
    Failed messages: 
        
    %s
    """;

    private List<OrganisationEntity> organisationEntitiesForTest;
    private GreenMailUser administratorMailUser;
    private GreenMailUser organisation1MailUser;

    @BeforeEach
    public void before() {
        String organisation1Email = "testiorg@test.com";
        String organisation2Email = "testiorg2@test.com";

        // test users
        administratorMailUser = greenMail.setUser(administratorEmail, "");
        organisation1MailUser = greenMail.setUser(organisation1Email, "");
        greenMail.setUser(organisation2Email, "");

        // user that the mailer uses
        GreenMailUser mailerUser = greenMail.setUser("test", "test");

        Mailer mailer = MailerBuilder
            .withSMTPServer("localhost", greenMail.getSmtp().getPort())
            .withSMTPServerUsername(mailerUser.getLogin())
            .withSMTPServerPassword(mailerUser.getPassword())
            .withTransportStrategy(TransportStrategy.SMTP)
            .buildMailer();

        emailSender = new EmailSender(organisationRepository, deadLetterQueueRepository, mailer, administratorEmail, noreplyEmail, true, subject, text, "local");

        organisationEntitiesForTest = new ArrayList<>();
        OrganisationEntity organisationEntity = new OrganisationEntity();
        organisationEntity.setId("ORG1");
        organisationEntity.setAdministratorEmail(organisation1Email);
        organisationEntity.setOrganisationName(new LocalisedString("TestiOrganisaatio 1", null, null));
        organisationEntity.setQueue("org1-jono");

        OrganisationEntity organisationEntity2 = new OrganisationEntity();
        organisationEntity2.setId("ORG2");
        organisationEntity2.setAdministratorEmail(organisation2Email);
        organisationEntity2.setOrganisationName(new LocalisedString("TestiOrganisaatio 2", null, null));
        organisationEntity2.setQueue("org2-jono");

        organisationEntitiesForTest.add(organisationEntity);
        organisationEntitiesForTest.add(organisationEntity2);
    }

    @Test
    public void testGetDeadLetterCountByMessageTypeStringList_withTwoMessageTypes_shouldSucceed() {
        List<DeadLetterQueueEntity> deadLetterQueueEntities = getDeadLetters();
        String stringList = emailSender.getDeadLetterCountByMessageTypeStringList(deadLetterQueueEntities);
        assertTrue(stringList.contains("UNKNOWN_MESSAGE: 2"));
        assertTrue(stringList.contains("CREATE_REGISTRATION_REQUEST: 1"));
    }

    @Test
    public void testEmailSending_forOneOrganisationAndWithTwoMessageTypes_shouldSucceed() throws Exception {
        Iterable<OrganisationEntity> organisationEntityIterable = organisationEntitiesForTest;
        when(organisationRepository.findAll()).thenReturn(organisationEntityIterable);

        List<DeadLetterQueueEntity> deadLetterQueueEntities = getDeadLetters();

        when(deadLetterQueueRepository.findByOrganisationIdAndEmailSent(
                eq("ORG1"), anyBoolean(), any())).thenReturn(deadLetterQueueEntities.stream());
        when(deadLetterQueueRepository.findByOrganisationIdAndEmailSent(
                eq("ORG2"), anyBoolean(), any())).thenReturn(Stream.empty());

        // send some emails
        emailSender.sendDeadLetterQueueEmail();
        assertEquals(2, greenMail.getReceivedMessages().length);

        // check that the correct users received the emails
        List<StoredMessage> organisation1UserEmails = greenMail.getManagers().getImapHostManager().getInbox(organisation1MailUser).getMessages();
        List<StoredMessage> administratorUserEmails = greenMail.getManagers().getImapHostManager().getInbox(administratorMailUser).getMessages();

        assertEquals(1, organisation1UserEmails.size());
        assertEquals(1, administratorUserEmails.size());

        Email bccAdminMessage = EmailConverter.mimeMessageToEmail(administratorUserEmails.get(0).getMimeMessage());
        Email actualOrganisationMessage = EmailConverter.mimeMessageToEmail(organisation1UserEmails.get(0).getMimeMessage());

        assertNotNull(bccAdminMessage, "BCC message is null");
        assertNotNull(actualOrganisationMessage, "Actual message is null");

        String bccMessageContent = bccAdminMessage.getPlainText();
        String actualMessageContent = actualOrganisationMessage.getPlainText();
        logger.info("BCC message content:\n{}", bccMessageContent);
        logger.info("Actual message content:\n{}", actualMessageContent);

        assertEquals(0, bccAdminMessage.getAttachments().size());
        assertEquals(0, actualOrganisationMessage.getAttachments().size());

        String deadLetterCountText = emailSender.getDeadLetterCountByMessageTypeStringList(deadLetterQueueEntities);

        // \r gets added to actual messages
        assertTrue(bccMessageContent.replaceAll("\\r", "").contains(deadLetterCountText));
        assertTrue(actualMessageContent.replaceAll("\\r", "").contains(deadLetterCountText));

        verify(deadLetterQueueRepository, times(1))
                .findByOrganisationIdAndEmailSent(eq("ORG1"), anyBoolean(), any());
        verify(deadLetterQueueRepository, times(1))
                .findByOrganisationIdAndEmailSent(eq("ORG2"), anyBoolean(), any());
        verify(deadLetterQueueRepository, times(deadLetterQueueEntities.size())).update(any());
    }

    private List<DeadLetterQueueEntity> getDeadLetters() {
        String messageType1 = "UNKNOWN_MESSAGE";
        String messageType2 = "CREATE_REGISTRATION_REQUEST";

        DeadLetterQueueEntity deadLetterQueueEntity = new DeadLetterQueueEntity();
        deadLetterQueueEntity.setEmailSent(false);
        deadLetterQueueEntity.setOrganisationId("ORG1");
        deadLetterQueueEntity.setMessageType(messageType1);
        deadLetterQueueEntity.setMessage("Testiviesti 1");

        DeadLetterQueueEntity deadLetterQueueEntity2 = new DeadLetterQueueEntity();
        deadLetterQueueEntity2.setEmailSent(false);
        deadLetterQueueEntity2.setOrganisationId("ORG1");
        deadLetterQueueEntity2.setMessageType(messageType1);
        deadLetterQueueEntity2.setMessage("Testiviesti 2");

        DeadLetterQueueEntity deadLetterQueueEntity3 = new DeadLetterQueueEntity();
        deadLetterQueueEntity3.setEmailSent(false);
        deadLetterQueueEntity3.setOrganisationId("ORG1");
        deadLetterQueueEntity3.setMessageType(messageType2);
        deadLetterQueueEntity3.setMessage("Testiviesti 3");

        return Arrays.asList(deadLetterQueueEntity, deadLetterQueueEntity2, deadLetterQueueEntity3);
    }
}
