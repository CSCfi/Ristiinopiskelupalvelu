package fi.uta.ristiinopiskelu.dlqhandler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.DeadLetterQueueRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Component
public class EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private final String ATTACHMENT_FILE_TYPE = ".json";

    private String administratorEmail;
    private String noreplyEmail;
    private boolean isEnabled;
    private String subject;
    private String body;

    private OrganisationRepository organisationRepository;
    private DeadLetterQueueRepository deadLetterQueueRepository;

    private JavaMailSender javaMailSender;
    private ObjectMapper objectMapper;

    @Autowired
    public EmailSender(OrganisationRepository organisationRepository,
                       DeadLetterQueueRepository deadLetterQueueRepository,
                       JavaMailSender javaMailSender,
                       ObjectMapper objectMapper,
                       @Value("${general.email.address.administrator}") String administratorEmail,
                       @Value("${general.email.address.noreply}") String noreplyEmail,
                       @Value("${general.emailsender.enabled}") boolean isEnabled,
                       @Value("${general.email.address.subject}") String subject,
                       @Value("${general.email.address.body}") String body) {
        this.organisationRepository = organisationRepository;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
        this.javaMailSender = javaMailSender;
        this.objectMapper = objectMapper;
        this.administratorEmail = administratorEmail;
        this.noreplyEmail = noreplyEmail;
        this.isEnabled = isEnabled;
        this.subject = subject;
        this.body = body;
    }

    @Scheduled(cron = "${general.emailsender.pollingRate}")
    public void sendDeadLetterQueueEmail() {
        if(!isEnabled) {
            return;
        }

        try {
            List<OrganisationEntity> organisations = StreamSupport.stream(organisationRepository.findAll().spliterator(), false).collect(Collectors.toList());

            for(OrganisationEntity organisation : organisations) {
                List<DeadLetterQueueEntity> deadLetters = deadLetterQueueRepository.findAllByOrganisationIdAndEmailSentOrderByConsumedTimestampAsc(organisation.getId(), false);

                if(CollectionUtils.isEmpty(deadLetters)) {
                    continue;
                }

                if(StringUtils.isEmpty(organisation.getAdministratorEmail())) {
                    logger.error("Organisation " + organisation.getId() + " is missing administrator email, cannot process forward!");
                    continue;
                }

                logger.info("Creating dead letter queue email for: " + organisation.getId() + " with email: " + organisation.getAdministratorEmail());
                MimeMessage message = createEmail(organisation.getAdministratorEmail(), deadLetters);
                javaMailSender.send(message);

                logger.info("Succesfully sent email");
                logger.info("Email attachment includes " + deadLetters.size() + " messages from dead letter queue.");
                logger.info("Email should include following attachments: " + String.join(", ", deadLetters.stream().map(dl -> dl.getMessageType() + ".json").distinct().collect(Collectors.toList())));

                markDeadLetterEmailSent(deadLetters);
            }

        } catch(Exception e) {
            logger.error("Unable to process dead letter queue and send email: ", e);
        }
    }

    private void markDeadLetterEmailSent(List<DeadLetterQueueEntity> deadLetters) {
        for (DeadLetterQueueEntity deadLetter : deadLetters) {
            deadLetter.setEmailSent(true);
            deadLetterQueueRepository.update(deadLetter);
        }
    }

    private MimeMessage createEmail(String organisationEmail, List<DeadLetterQueueEntity> deadLetters) throws Exception {
        MimeMessage msg = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(organisationEmail);
        helper.setBcc(administratorEmail);
        helper.setFrom(noreplyEmail);
        helper.setSubject(subject);
        helper.setText(body);

        Map<String, ByteArrayResource> attachments = createAttachments(deadLetters);

        for(Map.Entry<String, ByteArrayResource> attachment : attachments.entrySet()) {
            helper.addAttachment(attachment.getKey(), attachment.getValue());
        }

        return msg;
    }

    private Map<String, ByteArrayResource> createAttachments(List<DeadLetterQueueEntity> deadLetters) throws UnsupportedEncodingException, JsonProcessingException {
        Map<String, List<DeadLetterQueueEntity>> deadLettersByMessageType = deadLetters.stream()
                .collect(groupingBy(DeadLetterQueueEntity::getMessageType));


        Map<String, ByteArrayResource> attachments = new HashMap<>();
        for(Map.Entry<String, List<DeadLetterQueueEntity>> deadLetter : deadLettersByMessageType.entrySet()) {
            List<String> messages = new ArrayList<>();
            for(DeadLetterQueueEntity deadLetterQueueEntity : deadLetter.getValue()) {
                messages.add(deadLetterQueueEntity.getMessage());
            }

            Attachment attachment = new Attachment();
            attachment.setMessages(messages);
            attachment.setMessageType(deadLetter.getKey());

            String filename = deadLetter.getKey() + ATTACHMENT_FILE_TYPE;
            attachments.put(filename, new ByteArrayResource(objectMapper.writeValueAsString(attachment).getBytes("UTF-8")));
            logger.info("Added " + messages.size() + " messages to " + filename);
        }

        return attachments;
    }
}
