package fi.uta.ristiinopiskelu.dlqhandler.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.dlqhandler.processor.DeadLetterQueueProcessor;
import fi.uta.ristiinopiskelu.persistence.repository.DeadLetterQueueRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class DeadLetterQueueRoute extends AbstractRoute {
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueRoute.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private DeadLetterQueueRepository deadLetterQueueRepository;

    @Override
    protected List<RouteConfiguration> getConfigs() {
        List<OrganisationEntity> organisations = StreamSupport.stream(organisationRepository.findAll().spliterator(), false).collect(Collectors.toList());

        List<RouteConfiguration> routes = new ArrayList<>();
        for(OrganisationEntity organisation : organisations) {
            routes.add(new RouteConfiguration(
                    organisation.getQueue(),
                    new DeadLetterQueueProcessor(organisation.getId(), organisationRepository, deadLetterQueueRepository, objectMapper)));
        }

        return routes;
    }
}
