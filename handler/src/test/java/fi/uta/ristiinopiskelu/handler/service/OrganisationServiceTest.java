package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.service.impl.OrganisationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
public class OrganisationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(NetworkServiceTest.class);

    //@MockBean
    //private OrganisationService organisationService;

    private ModelMapper modelMapper = new ModelMapper();

    @BeforeEach
    public void setUp() {
        this.modelMapper.createTypeMap(OrganisationEntity.class, Organisation.class)
                .addMappings(mapper -> mapper.map(OrganisationEntity::getId, Organisation::setOrganisationTkCode));
    }

    //private Map<String, Organisation> testOrganisations = null;
    //private List<String> orgIdentifier;

    @Test
    public void testFillingMissingValuesFromEntity_shouldSucceed() {
        OrganisationEntity entity = new OrganisationEntity();
        entity.setOrganisationName(new LocalisedString("testi", null, null));
        entity.setId("ORG1");

        Organisation dto = new Organisation();
        dto.setOrganisationTkCode("ORG1");

        Organisation decorated = new OrganisationServiceImpl().fillMissingValues(dto, entity);

        Organisation dtoFromEntity = modelMapper.map(entity, Organisation.class);

        System.out.println("DTO");
        System.out.println(decorated.toString());
        System.out.println("---------------------------------------");
        System.out.println("Entity");
        System.out.println("---------------------------------------");
        System.out.print(dtoFromEntity.toString());

        assertTrue(decorated.equals(dtoFromEntity));
    }

    @Test
    public void testFillingMissingValuesFromEntityButNoValuesOverwritten_shouldSucceed() {
        LocalisedString originalEntityNameDescription = new LocalisedString("Name description of entity, should be copied to current", null, null);

        OrganisationEntity entity = new OrganisationEntity();
        entity.setOrganisationName(new LocalisedString("Name of entity", null, null));
        entity.setNameDescription(originalEntityNameDescription);
        entity.setId("ORG1");

        LocalisedString originalDtoName = new LocalisedString("This name came with the DTO, should not be overwritten", null, null);

        Organisation dto = new Organisation();
        dto.setOrganisationTkCode("ORG1");
        dto.setOrganisationName(originalDtoName);
        
        dto = new OrganisationServiceImpl().fillMissingValues(dto, entity);

        Organisation dtoFromEntity = modelMapper.map(entity, Organisation.class);

        System.out.println("DTO");
        System.out.println(dto.toString());
        System.out.println("---------------------------------------");
        System.out.println("Entity");
        System.out.println("---------------------------------------");
        System.out.print(dtoFromEntity.toString());

        assertEquals(originalDtoName.getValue("fi"), dto.getOrganisationName().getValue("fi"));
        assertEquals(originalEntityNameDescription.getValue("fi"), dto.getNameDescription().getValue("fi"));
    }
}
