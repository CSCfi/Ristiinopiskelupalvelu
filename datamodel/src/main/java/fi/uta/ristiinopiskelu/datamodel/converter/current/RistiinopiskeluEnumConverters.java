package fi.uta.ristiinopiskelu.datamodel.converter.current;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Country;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightStatusValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.StudyRightType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.GradeCode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.ScaleValue;
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
    private static class StudyRightTypeWritingConverter implements Converter<StudyRightType, String> {

        @Override
        public String convert(StudyRightType studyRightType) {
            return studyRightType.getCode();
        }
    }

    @ReadingConverter
    private static class StudyRightTypeReadingConverter implements Converter<String, StudyRightType> {

        @Override
        public StudyRightType convert(String code) {
            return StudyRightType.fromValue(code);
        }
    }

    @WritingConverter
    private static class GradeCodeWritingConverter implements Converter<GradeCode, String> {

        @Override
        public String convert(GradeCode gradeCode) {
            return gradeCode.getCode();
        }
    }

    @ReadingConverter
    private static class GradeCodeReadingConverter implements Converter<String, GradeCode> {

        @Override
        public GradeCode convert(String code) {
            return GradeCode.fromValue(code);
        }
    }

    @WritingConverter
    private static class CountryWritingConverter implements Converter<Country, String> {

        @Override
        public String convert(Country country) {
            return country.getCode();
        }
    }

    @ReadingConverter
    private static class CountryReadingConverter implements Converter<String, Country> {

        @Override
        public Country convert(String code) {
            return Country.fromValue(code);
        }
    }

    @WritingConverter
    private static class OrganisationRoleWritingConverter implements Converter<OrganisationRole, Integer> {

        @Override
        public Integer convert(OrganisationRole organisationRole) {
            return organisationRole.getCode();
        }
    }

    @ReadingConverter
    private static class OrganisationRoleReadingConverter implements Converter<Integer, OrganisationRole> {

        @Override
        public OrganisationRole convert(Integer integer) {
            return OrganisationRole.fromValue(integer);
        }
    }

    @WritingConverter
    private static class LanguageWritingConverter implements Converter<Language, String> {

        @Override
        public String convert(Language language) {
            return language.getValue();
        }
    }

    @ReadingConverter
    private static class LanguageReadingConverter implements Converter<String, Language> {

        @Override
        public Language convert(String s) {
            return Language.fromValue(s);
        }
    }

    @WritingConverter
    private static class CompletedCreditTypeWritingConverter implements Converter<CompletedCreditType, Integer> {

        @Override
        public Integer convert(CompletedCreditType value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class CompletedCreditTypeReadingConverter implements Converter<Integer, CompletedCreditType> {

        @Override
        public CompletedCreditType convert(Integer value) {
            return CompletedCreditType.fromValue(value);
        }
    }

    @WritingConverter
    private static class MinEduGuidanceAreaWritingConverter implements Converter<MinEduGuidanceArea, Integer> {

        @Override
        public Integer convert(MinEduGuidanceArea value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class MinEduGuidanceAreaReadingConverter implements Converter<Integer, MinEduGuidanceArea> {

        @Override
        public MinEduGuidanceArea convert(Integer value) {
            return MinEduGuidanceArea.fromValue(value);
        }
    }

    @WritingConverter
    private static class RegistrationSelectionItemStatusWritingConverter implements Converter<RegistrationSelectionItemStatus, Integer> {

        @Override
        public Integer convert(RegistrationSelectionItemStatus value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class RegistrationSelectionItemStatusReadingConverter implements Converter<Integer, RegistrationSelectionItemStatus> {

        @Override
        public RegistrationSelectionItemStatus convert(Integer value) {
            return RegistrationSelectionItemStatus.fromValue(value);
        }
    }

    @WritingConverter
    private static class ScaleValueWritingConverter implements Converter<ScaleValue, Integer> {

        @Override
        public Integer convert(ScaleValue value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class ScaleValueReadingConverter implements Converter<Integer, ScaleValue> {

        @Override
        public ScaleValue convert(Integer value) {
            return ScaleValue.fromValue(value);
        }
    }

    @WritingConverter
    private static class StudyRightStatusValueWritingConverter implements Converter<StudyRightStatusValue, Integer> {

        @Override
        public Integer convert(StudyRightStatusValue value) {
            return value.getCode();
        }
    }

    @ReadingConverter
    private static class StudyRightStatusValueReadingConverter implements Converter<Integer, StudyRightStatusValue> {

        @Override
        public StudyRightStatusValue convert(Integer value) {
            return StudyRightStatusValue.fromValue(value);
        }
    }
}
