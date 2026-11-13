package fi.uta.ristiinopiskelu.handler.integration.route.current;

import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRouteIntegrationTest.class);

    @Autowired
    private NetworkRepository networkRepository;

    protected NetworkEntity persistNetworkEntity(String id, LocalisedStringEntity name, List<String> organisationIds) {
        List<NetworkOrganisationEntity> networkOrgs = new ArrayList<>();
        for (String orgId : organisationIds) {
            NetworkOrganisationEntity networkOrganisation = new NetworkOrganisationEntity();
            networkOrganisation.setOrganisationTkCode(orgId);
            networkOrganisation.setValidityInNetwork(EntityInitializer.getIndefinitelyValidity(OffsetDateTime.now()));
            networkOrganisation.setIsCoordinator(true);
            networkOrgs.add(networkOrganisation);
        }
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(id, name, networkOrgs,
                EntityInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        return networkRepository.create(networkEntity);
    }
}

    
