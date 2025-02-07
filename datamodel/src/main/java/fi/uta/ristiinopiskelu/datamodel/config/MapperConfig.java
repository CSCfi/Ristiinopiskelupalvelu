package fi.uta.ristiinopiskelu.datamodel.config;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeSet;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta.VirtaCode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta.VirtaCodeSet;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta.VirtaCodeValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.organisation.OrganisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.registration.RegistrationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.code.CodeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.organisation.OrganisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.registration.RegistrationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.student.StudentWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.degree.DegreeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyrecord.StudyRecordWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.CourseUnit;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Degree;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyElement;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyModule;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateCourseUnitRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateDegreeRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyElementRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyModuleRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.createTypeMap(VirtaCode.class, CodeEntity.class)
                .addMappings(mapper -> mapper.map(VirtaCode::getKoodiUri, CodeEntity::setCodeUri))
                .addMappings(mapper -> mapper.map(VirtaCode::getKoodisto, CodeEntity::setCodeSet))
                .addMappings(mapper -> mapper.map(VirtaCode::getKoodiArvo, CodeEntity::setKey))
                .addMappings(mapper -> mapper.map(VirtaCode::getVersio, CodeEntity::setCodeVersion))
                .addMappings(mapper -> mapper.map(VirtaCode::getPaivitysPvm, CodeEntity::setUpdateDate))
                .addMappings(mapper -> mapper.map(VirtaCode::getVoimassaAlkuPvm, CodeEntity::setValidityStartDate))
                .addMappings(mapper -> mapper.map(VirtaCode::getVoimassaLoppuPvm, CodeEntity::setValidityEndDate))
                .addMappings(mapper -> mapper.map(VirtaCode::getTila, CodeEntity::setStatus))
                .addMappings(mapper -> mapper.map(VirtaCode::getMetadata, CodeEntity::setCodeValues));

        modelMapper.createTypeMap(VirtaCodeSet.class, CodeSet.class)
                .addMappings(mapper -> mapper.map(VirtaCodeSet::getKoodistoUri, CodeSet::setKey))
                .addMappings(mapper -> mapper.map(VirtaCodeSet::getKoodistoVersio, CodeSet::setVersion))
                .addMappings(mapper -> mapper.map(VirtaCodeSet::getOrganisaatioOid, CodeSet::setOrganisationOid));

        modelMapper.createTypeMap(VirtaCodeValue.class, CodeValue.class)
                .addMappings(mapper -> mapper.map(VirtaCodeValue::getKieli, CodeValue::setLanguage))
                .addMappings(mapper -> mapper.map(VirtaCodeValue::getKuvaus, CodeValue::setDescription))
                .addMappings(mapper -> mapper.map(VirtaCodeValue::getLyhytNimi, CodeValue::setAbbreviation))
                .addMappings(mapper -> mapper.map(VirtaCodeValue::getNimi, CodeValue::setValue));

        modelMapper.createTypeMap(RegistrationEntity.class, RegistrationReadDTO.class)
                .addMappings(mapper -> mapper.map(RegistrationEntity::getId, RegistrationReadDTO::setRegistrationRequestId));

        modelMapper.createTypeMap(OrganisationWriteDTO.class, OrganisationEntity.class)
                .addMappings(mapper -> mapper.map(OrganisationWriteDTO::getOrganisationTkCode, OrganisationEntity::setId));

        modelMapper.createTypeMap(OrganisationEntity.class, OrganisationReadDTO.class)
                .addMappings(mapper -> mapper.map(OrganisationEntity::getId, OrganisationReadDTO::setOrganisationTkCode));

        // make sure that database id (get/setId()) of these entities is never mapped, field is not present on the matching DTOs
        modelMapper.createTypeMap(CodeWriteDTO.class, CodeEntity.class)
                .addMappings(mapper -> mapper.skip(CodeEntity::setId));

        modelMapper.createTypeMap(RegistrationWriteDTO.class, RegistrationEntity.class)
            .addMappings(mapper -> mapper.skip(RegistrationEntity::setId));

        modelMapper.createTypeMap(StudentWriteDTO.class, StudentEntity.class)
            .addMappings(mapper -> mapper.skip(StudentEntity::setId));

        modelMapper.createTypeMap(StudyRecordWriteDTO.class, StudyRecordEntity.class)
            .addMappings(mapper -> mapper.skip(StudyRecordEntity::setId));

        modelMapper.createTypeMap(AbstractStudyElementWriteDTO.class, StudyElementEntity.class)
            .addMappings(mapper -> mapper.skip(StudyElementEntity::setId));

        modelMapper.createTypeMap(CourseUnitWriteDTO.class, CourseUnitEntity.class)
            .includeBase(AbstractStudyElementWriteDTO.class, StudyElementEntity.class)
            .addMappings(mapper -> mapper.skip(CourseUnitEntity::setId));

        modelMapper.createTypeMap(DegreeWriteDTO.class, DegreeEntity.class)
            .includeBase(AbstractStudyElementWriteDTO.class, StudyElementEntity.class)
            .addMappings(mapper -> mapper.skip(DegreeEntity::setId));

        modelMapper.createTypeMap(StudyModuleWriteDTO.class, StudyModuleEntity.class)
            .includeBase(AbstractStudyElementWriteDTO.class, StudyElementEntity.class)
            .addMappings(mapper -> mapper.skip(StudyModuleEntity::setId));

        modelMapper.createTypeMap(RealisationWriteDTO.class, RealisationEntity.class)
            .addMappings(mapper -> mapper.skip(RealisationEntity::setId));

        // v8 mappings
        modelMapper.createTypeMap(CreateStudyElementRequestDTO.class, StudyElement.class);

        modelMapper.createTypeMap(CreateCourseUnitRequestDTO.class, CourseUnit.class)
            .includeBase(CreateStudyElementRequestDTO.class, StudyElement.class);

        modelMapper.createTypeMap(CreateStudyModuleRequestDTO.class, StudyModule.class)
            .includeBase(CreateStudyElementRequestDTO.class, StudyElement.class);

        modelMapper.createTypeMap(CreateDegreeRequestDTO.class, Degree.class)
            .includeBase(CreateStudyElementRequestDTO.class, StudyElement.class);

        modelMapper.typeMap(CreateCourseUnitRequestDTO.class, StudyElement.class)
            .setConverter(converterWithDestinationSupplier(CourseUnit::new));

        modelMapper.typeMap(CreateDegreeRequestDTO.class, StudyElement.class)
            .setConverter(converterWithDestinationSupplier(Degree::new));

        modelMapper.typeMap(CreateStudyModuleRequestDTO.class, StudyElement.class)
            .setConverter(converterWithDestinationSupplier(StudyModule::new));

        return modelMapper;
    }

    private <S, D> Converter<S, D> converterWithDestinationSupplier(Supplier<? extends D> supplier ) {
        return ctx -> ctx.getMappingEngine().map(ctx.create(ctx.getSource(), supplier.get()));
    }
}
