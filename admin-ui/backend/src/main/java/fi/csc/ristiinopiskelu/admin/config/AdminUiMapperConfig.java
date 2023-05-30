package fi.csc.ristiinopiskelu.admin.config;

import fi.csc.ristiinopiskelu.admin.dto.CreateOrUpdateOrganisationDTO;
import fi.csc.ristiinopiskelu.admin.dto.OrganisationDTO;
import fi.uta.ristiinopiskelu.datamodel.config.MapperConfig;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminUiMapperConfig extends MapperConfig {

    @Bean
    @Override
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = super.modelMapper();

        // additional mapper configuration for OrganisationDTO in order to make sure schemaVersion and version fields don't mix
        modelMapper.createTypeMap(OrganisationEntity.class, OrganisationDTO.class)
                .addMappings(mapper -> mapper.map(OrganisationEntity::getSchemaVersion, OrganisationDTO::setSchemaVersion));

        modelMapper.createTypeMap(CreateOrUpdateOrganisationDTO.class, OrganisationEntity.class)
            .addMappings(mapper -> mapper.map(CreateOrUpdateOrganisationDTO::getSchemaVersion, OrganisationEntity::setSchemaVersion))
            .addMappings(mapper -> mapper.skip(OrganisationEntity::setVersion));

        modelMapper.createTypeMap(OrganisationDTO.class, OrganisationEntity.class)
            .includeBase(CreateOrUpdateOrganisationDTO.class, OrganisationEntity.class);

        return modelMapper;
    }
}
