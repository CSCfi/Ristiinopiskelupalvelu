package fi.uta.ristiinopiskelu.datamodel.converter.current;

import fi.uta.ristiinopiskelu.datamodel.entity.common.CountryEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LanguageEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.OrganisationRoleEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightStatusValueEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationSelectionItemStatusEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.StudyRightTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.GradeCodeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.MinEduGuidanceAreaEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.ScaleValueEntity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.Arrays;
import java.util.List;

public class RistiinopiskeluEnumConverters {

    public static List<Converter<?, ?>> getConverters() {
        return Arrays.asList(
            new StudyRightTypeReadingConverter(), new StudyRightTypeWritingConverter(),
            new GradeCodeReadingConverter(), new GradeCodeWritingConverter(),
            new CountryReadingConverter(), new CountryWritingConverter(),
            new OrganisationRoleReadingConverter(), new OrganisationRoleWritingConverter(),
            new LanguageReadingConverter(), new LanguageWritingConverter(),
            new CompletedCreditTypeReadingConverter(), new CompletedCreditTypeWritingConverter(),
            new MinEduGuidanceAreaReadingConverter(), new MinEduGuidanceAreaWritingConverter(),
            new RegistrationSelectionItemStatusReadingConverter(), new RegistrationSelectionItemStatusWritingConverter(),
            new ScaleValueReadingConverter(), new ScaleValueWritingConverter(),
            new StudyRightStatusValueReadingConverter(), new StudyRightStatusValueWritingConverter());
    }

    @WritingConverter
    private static class StudyRightTypeWritingConverter implements Converter<StudyRightTypeEntity, String> {

        @Override
        public String convert(StudyRightTypeEntity studyRightType) {
            return studyRightType.getCode();
        }
    }

    @ReadingConverter
    private static class StudyRightTypeReadingConverter implements Converter<String, StudyRightTypeEntity> {

        @Override
        public StudyRightTypeEntity convert(String code) {
            return StudyRightTypeEntity.fromValue(code);
        }
    }

    @WritingConverter
    private static class GradeCodeWritingConverter implements Converter<GradeCodeEntity, String> {

        @Override
        public String convert(GradeCodeEntity gradeCode) {
            return gradeCode.getCode();
        }
    }

    @ReadingConverter
    private static class GradeCodeReadingConverter implements Converter<String, GradeCodeEntity> {

        @Override
        public GradeCodeEntity convert(String code) {
            return GradeCodeEntity.fromValue(code);
        }
    }

    @WritingConverter
    private static class CountryWritingConverter implements Converter<CountryEntity, String> {

        @Override
        public String convert(CountryEntity country) {
            return country.getCode();
        }
    }

    @ReadingConverter
    private static class CountryReadingConverter implements Converter<String, CountryEntity> {

        @Override
        public CountryEntity convert(String code) {
            return CountryEntity.fromValue(code);
        }
    }

    @WritingConverter
    private static class OrganisationRoleWritingConverter implements Converter<OrganisationRoleEntity, Integer> {

        @Override
        public Integer convert(OrganisationRoleEntity organisationRole) {
            return organisationRole.getCode();
        }
    }

    @ReadingConverter
    private static class OrganisationRoleReadingConverter implements Converter<Integer, OrganisationRoleEntity> {

        @Override
        public OrganisationRoleEntity convert(Integer integer) {
            return OrganisationRoleEntity.fromValue(integer);
        }
    }

    @WritingConverter
    private static class LanguageWritingConverter implements Converter<LanguageEntity, String> {

        @Override
        public String convert(LanguageEntity language) {
            return language.getValue();
        }
    }

    @ReadingConverter
    private static class LanguageReadingConverter implements Converter<String, LanguageEntity> {

        @Override
        public LanguageEntity convert(String s) {
            return LanguageEntity.fromValue(s);
        }
    }

    @WritingConverter
    private static class CompletedCreditTypeWritingConverter implements Converter<CompletedCreditTypeEntity, Integer> {

        @Override
        public Integer convert(CompletedCreditTypeEntity value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class CompletedCreditTypeReadingConverter implements Converter<Integer, CompletedCreditTypeEntity> {

        @Override
        public CompletedCreditTypeEntity convert(Integer value) {
            return CompletedCreditTypeEntity.fromValue(value);
        }
    }

    @WritingConverter
    private static class MinEduGuidanceAreaWritingConverter implements Converter<MinEduGuidanceAreaEntity, Integer> {

        @Override
        public Integer convert(MinEduGuidanceAreaEntity value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class MinEduGuidanceAreaReadingConverter implements Converter<Integer, MinEduGuidanceAreaEntity> {

        @Override
        public MinEduGuidanceAreaEntity convert(Integer value) {
            return MinEduGuidanceAreaEntity.fromValue(value);
        }
    }

    @WritingConverter
    private static class RegistrationSelectionItemStatusWritingConverter implements Converter<RegistrationSelectionItemStatusEntity, Integer> {

        @Override
        public Integer convert(RegistrationSelectionItemStatusEntity value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class RegistrationSelectionItemStatusReadingConverter implements Converter<Integer, RegistrationSelectionItemStatusEntity> {

        @Override
        public RegistrationSelectionItemStatusEntity convert(Integer value) {
            return RegistrationSelectionItemStatusEntity.fromValue(value);
        }
    }

    @WritingConverter
    private static class ScaleValueWritingConverter implements Converter<ScaleValueEntity, Integer> {

        @Override
        public Integer convert(ScaleValueEntity value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class ScaleValueReadingConverter implements Converter<Integer, ScaleValueEntity> {

        @Override
        public ScaleValueEntity convert(Integer value) {
            return ScaleValueEntity.fromValue(value);
        }
    }

    @WritingConverter
    private static class StudyRightStatusValueWritingConverter implements Converter<StudyRightStatusValueEntity, Integer> {

        @Override
        public Integer convert(StudyRightStatusValueEntity value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class StudyRightStatusValueReadingConverter implements Converter<Integer, StudyRightStatusValueEntity> {

        @Override
        public StudyRightStatusValueEntity convert(Integer value) {
            return StudyRightStatusValueEntity.fromValue(value);
        }
    }
}
