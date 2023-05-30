package fi.csc.ristiinopiskelu.admin.config;

import fi.csc.ristiinopiskelu.admin.dto.CreateOrUpdateOrganisationDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModelMapperTest {

    @Test
    public void testModelMapper_schemaVersionIsSetCorrectlyWhenMappingOrganisationDtoToEntity_shouldSucceed() {
        AdminUiMapperConfig mapperConfig = new AdminUiMapperConfig();
        ModelMapper mapper = mapperConfig.modelMapper();

        CreateOrUpdateOrganisationDTO organisation = new CreateOrUpdateOrganisationDTO();
        organisation.setSchemaVersion(8);

        OrganisationEntity testEntity = new OrganisationEntity();
        testEntity.setVersion(1L);
        testEntity.setSchemaVersion(5);

        mapper.map(organisation, testEntity);
        assertEquals(1, testEntity.getVersion());
        assertEquals(8, testEntity.getSchemaVersion());
    }

    @Test
    public void testModelMapper_schemaVersionIsSetCorrectlyWhenMappingOrganisationEntityToDto_shouldSucceed() {
        AdminUiMapperConfig mapperConfig = new AdminUiMapperConfig();
        ModelMapper mapper = mapperConfig.modelMapper();

        OrganisationEntity testEntity = new OrganisationEntity();
        testEntity.setVersion(1L);
        testEntity.setSchemaVersion(8);

        CreateOrUpdateOrganisationDTO organisation = mapper.map(testEntity, CreateOrUpdateOrganisationDTO.class);
        assertEquals(8, organisation.getSchemaVersion());
    }
}
