package fi.uta.ristiinopiskelu.dlqhandler.component;

import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.DeadLetterQueueRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Component
public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private final String UNKNOWN_ENVIRONMENT = "unknown";

    private final Map<String, String> environmentTranslations = Map.of(
        "local", "Paikallinen kehitysympäristö",
        "develop", "Kehitysympäristö",
        "staging", "Testiympäristö",
        "production", "Tuotanto",
        UNKNOWN_ENVIRONMENT, "Tuntematon"
    );

    private String administratorEmail;
    private String noreplyEmail;
    private boolean isEnabled;
    private String subject;
    private String body;
    private String environment;

    private OrganisationRepository organisationRepository;
    private DeadLetterQueueRepository deadLetterQueueRepository;

    private Mailer mailer;

    @Autowired
    public EmailSender(OrganisationRepository organisationRepository,
                       DeadLetterQueueRepository deadLetterQueueRepository,
                       Mailer mailer,
                       @Value("${general.email.address.administrator}") String administratorEmail,
                       @Value("${general.email.address.noreply}") String noreplyEmail,
                       @Value("${general.emailsender.enabled}") boolean isEnabled,
                       @Value("${general.email.address.subject}") String subject,
                       @Value("${general.email.address.body}") String body,
                       @Value("${general.activemq.environment}") String environment) {
        this.organisationRepository = organisationRepository;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
        this.mailer = mailer;
        this.administratorEmail = administratorEmail;
        this.noreplyEmail = noreplyEmail;
        this.isEnabled = isEnabled;
        this.subject = subject;
        this.body = body;
        this.environment = environment;
    }

    @Scheduled(cron = "${general.emailsender.pollingRate}")
    public void sendDeadLetterQueueEmail() {
        if (!isEnabled) {
            return;
        }

        try {
            List<OrganisationEntity> organisations = StreamSupport.stream(organisationRepository.findAll().spliterator(), false).toList();
            PageRequest pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Order.asc("consumedTimestamp")));

            for (OrganisationEntity organisation : organisations) {
                List<DeadLetterQueueEntity> deadLetters;

                try(Stream<DeadLetterQueueEntity> stream = deadLetterQueueRepository.findByOrganisationIdAndEmailSent(
                        organisation.getId(), false, pageRequest)) {
                    deadLetters = stream.toList();
                }

                if (CollectionUtils.isEmpty(deadLetters)) {
                    continue;
                }

                if (!StringUtils.hasText(organisation.getAdministratorEmail())) {
                    logger.error("Organisation {} is missing administrator email, cannot process forward!", organisation.getId());
                    continue;
                }

                logger.info("Creating dead letter queue email for organisation {} with email address {}", organisation.getId(),
                        organisation.getAdministratorEmail());
                mailer.sendMail(createEmail(organisation.getAdministratorEmail(), deadLetters));

                logger.info("Successfully sent email, email includes {} messages from dead letter queue.", deadLetters.size());
                markDeadLetterEmailSent(deadLetters);
            }

        } catch (Exception e) {
            logger.error("Unable to process dead letter queue and send email", e);
        }
    }

    private void markDeadLetterEmailSent(List<DeadLetterQueueEntity> deadLetters) {
        for (DeadLetterQueueEntity deadLetter : deadLetters) {
            deadLetter.setEmailSent(true);
            deadLetterQueueRepository.update(deadLetter);
        }
    }

    private Email createEmail(String organisationEmail, List<DeadLetterQueueEntity> deadLetters) {
        return EmailBuilder.startingBlank()
            .from(noreplyEmail)
            .to(organisationEmail)
            .bcc(administratorEmail)
            .withSubject(subject)
            .withPlainText(body.formatted(getEnvironmentText(), getDeadLetterCountByMessageTypeStringList(deadLetters)))
            .buildEmail();
    }

    private String getEnvironmentText() {
        String environmentText = environmentTranslations.get(environment);

        if(!StringUtils.hasText(environmentText)) {
            return environmentTranslations.get(UNKNOWN_ENVIRONMENT);
        }

        return environmentText;
    }

    protected String getDeadLetterCountByMessageTypeStringList(List<DeadLetterQueueEntity> deadLetters) {
        Map<String, Long> deadLetterCountByMessageType = deadLetters.stream()
            .collect(groupingBy(DeadLetterQueueEntity::getMessageType, Collectors.counting()));

        return deadLetterCountByMessageType.entrySet().stream()
            .map(entry -> "%s: %s".formatted(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("\n"));
    }
}
