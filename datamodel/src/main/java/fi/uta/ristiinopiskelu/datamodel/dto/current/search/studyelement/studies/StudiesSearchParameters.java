package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.PageableSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "StudiesSearchParametersV9", title = "StudiesSearchParametersV9")
public class StudiesSearchParameters extends PageableSearchParameters<StudyElementEntity> {

    @Schema(description = "Rajaa tarjontaa nimen tai koodin perusteella.")
    private String query;

    @Schema(description = "Rajaa palautettuja opintojaksoja niihin liittyvien toteutusten nimen tai koodin perusteella.")
    private String realisationQuery;

    @Schema(description = "Rajaa tarjontaa verkostojen mukaan.")
    private List<String> networkIdentifiers;

    @Schema(description = "Rajaa tarjontaa järjestäjien mukaan.")
    private List<String> organizingOrganisationIdentifiers;

    @Schema(description = "Rajaa palautettuja opintojaksoja niihin liittyvien toteutusten alkuajan mukaan." +
        " Palautetaan sellaisia opintojaksoja, joihin liittyy vähintään yksi toteutus, jossa alkuaika on pienempi tai yhtäsuuri kuin annettu arvo.")
    private LocalDate realisationStartDate;

    @Schema(description = "Rajaa palautettuja opintojaksoja siihen liittyvien toteutusten päättymisajan mukaan." +
        " Palautetaan sellaisia opintojaksoja, joihin liittyy vähintään yksi toteutus, jossa päättymisaika on suurempi tai yhtäsuuri kuin annettu arvo tai päättymisaikaa ei ole annettu.")
    private LocalDate realisationEndDate;

    @Schema(description = "Rajaa palautettuja opintojaksoja siihen liittyvien toteutusten ilmoittautumisen alkamisajan mukaan." +
        " Palautetaan sellaisia opintojaksoja, joihin liittyy vähintään yksi toteutus, jossa ilmoittautumisen alkamisaika on pienempi tai yhtäsuuri kuin annettu arvo." +
        " Tämä parametri on vanhentunut, ja tämän sijaan tulisi käyttää uusia From/To variantteja. Mikäli mikään uusista From/To parametreista alkamis- tai päättymisajalle" +
        " on annettu, jätetään molemmat realisationEnrollmentStartDateTime ja realisationEnrollmentEndDateTime huomiotta.")
    @Deprecated
    private OffsetDateTime realisationEnrollmentStartDateTime;

    @Schema(description = "Rajaa palautettuja opintojaksoja siihen liittyvien toteutusten ilmoittautumisen päättymisajan mukaan." +
        " Palautetaan sellaisia opintojaksoja, joihin liittyy vähintään yksi toteutus, jossa ilmoittautumisen päättymisesaika on suurempi" +
        " tai yhtäsuuri kuin annettu arvo tai päättymisaikaa ei ole annettu." +
        " Tämä parametri on vanhentunut, ja tämän sijaan tulisi käyttää uusia From/To variantteja. Mikäli mikään uusista From/To parametreista alkamis- tai päättymisajalle" +
        " on annettu, jätetään molemmat realisationEnrollmentStartDateTime ja realisationEnrollmentEndDateTime huomiotta.")
    @Deprecated
    private OffsetDateTime realisationEnrollmentEndDateTime;

    @Schema(description = "Jos jokin parametreista realisationEnrollment<Start|End>DateTime<From|To> on annettu, rajataan palautettavia opintojaksoja siten," +
        " että niihin tulee liittyä vähintään yksi toteutus, jonka ilmoittautumisaika täyttää annetut ehdot. Tämä parametri rajaa sitä, milloin aikaisintaan" +
        " ilmoittautuminen saa alkaa: realisationEnrollmentStartDateTimeFrom <= alkamisaika. Jos tätä parametria ei ole annettu, saa ilmoittautuminen" +
        " alkaa kuinka aikaisin vain.")
    private OffsetDateTime realisationEnrollmentStartDateTimeFrom;

    @Schema(description = "Jos jokin parametreista realisationEnrollment<Start|End>DateTime<From|To> on annettu, rajataan palautettavia opintojaksoja siten," +
        " että niihin tulee liittyä vähintään yksi toteutus, jonka ilmoittautumisaika täyttää annetut ehdot. Tämä parametri rajaa sitä, milloin viimeistään" +
        " ilmoittautumisen tulee alkaa: alkamisaika <= realisationEnrollmentStartDateTimeTo. Jos tätä parametria ei ole annettu, saa ilmoittautuminen" +
        " alkaa kuinka kuinka myöhään vain.")
    private OffsetDateTime realisationEnrollmentStartDateTimeTo;

    @Schema(description = "Jos jokin parametreista realisationEnrollment<Start|End>DateTime<From|To> on annettu, rajataan palautettavia opintojaksoja siten," +
        " että niihin tulee liittyä vähintään yksi toteutus, jonka ilmoittautumisaika täyttää annetut ehdot. Tämä parametri rajaa sitä, milloin aikaisintaan" +
        " ilmoittautuminen saa päättyä: realisationEnrollmentEndDateTimeFrom <= päättymisaika. Jos toteutuksen ilmoittautumisajalla ei ole" +
        " päättymisaikaa, se kelpaa tälle ehdolle. Jos tätä parametria ei ole annettu, saa ilmoittautuminen päättyä kuinka aikaisin vain.")
    private OffsetDateTime realisationEnrollmentEndDateTimeFrom;

    @Schema(description = "Jos jokin parametreista realisationEnrollment<Start|End>DateTime<From|To> on annettu, rajataan palautettavia opintojaksoja siten," +
        " että niihin tulee liittyä vähintään yksi toteutus, jonka ilmoittautumisaika täyttää annetut ehdot. Tämä parametri rajaa sitä, milloin viimeistään" +
        " ilmoittautumisen tulee päättyä: päättymisaika <= realisationEnrollmentEndDateTimeTo. Jos toteutuksen ilmoittautumisajalla ei ole" +
        " päättymisaikaa, se EI kelpaa tälle ehdolle. Jos tätä parametria ei ole annettu, saa ilmoittautuminen jatkua kuinka pitkään vain (esimerkiksi niin," +
        " että päättymisaika puuttuu kokonaan).")
    private OffsetDateTime realisationEnrollmentEndDateTimeTo;

    @Schema(description = "Rajaa palautetetaanko hakuehtoihin osuvia opintojaksoja, opintokokonaisuuksia, tutkintoja, vai kaikkia. Oletuksena palautetaan kaikkia.")
    private StudiesSearchElementType type = StudiesSearchElementType.ALL;

    @Schema(description = "Oletusarvo: NONE (ei minkään kentän mukaan). HUOM! Arvo NAME järjestää 'language' -parametrin mukaisella kielellä")
    private StudiesSearchSortField sortBy = StudiesSearchSortField.NONE;

    @Schema(description = "Oletusarvo: ASC (nouseva)")
    private Sort.Direction sortDirection = Sort.Direction.ASC;

    @Schema(description = "Palautetaanko omaa tarjontaa. Oletusarvo: false")
    private boolean includeOwn = false;

    @Schema(description = "Palautetaanko vain voimassaolevaa tarjontaa voimassaolevista verkostoista. Oletusarvo: false")
    private boolean includeInactive = false;

    @Schema(description = "Palautetaanko toteutushakuehdoista riippumatta myös opintojaksoja, joilla ei ole yhtään aktiivisia toteutuksia. Oletusarvo: false")
    private boolean includeCourseUnitsWithoutActiveRealisations = false;

    @Schema(description = "Määrittää 'query', 'realisationQuery' ja 'sortBy' -hakuehtojen kielen. Mahdolliset arvot: fi, sv, en. Oletusarvo: fi")
    private Language language = Language.FI;

    @Schema(description = "Kohdistetaanko nimi-haut (query ja realisationQuery) kaikkiin kieliversioihin. Jos true, niin parametri language määrittää silti NAME pohjaiseen järjestämiseen käytetyn kielen. Oletusarvo: false")
    private boolean langIndependent = false;

    @Schema(description = "Palautetaanko vain sellaiset opintojaksot, joihin liittyy vähintään yksi toteutus, jossa " +
        "ilmoittautuminen ei ole suljettu ('enrollmentClosed' = false) JA verkostoliitos on voimassa. Oletusarvo: false")
    private boolean onlyEnrollable = false;

    @Schema(description = "Rajaa tarjontaa kokonaisuudelle tai opintojaksolle määritellyn opetuskielen mukaan. Oletuksena palautetaan kaikilla opetuskielillä olevat tarjonnat.")
    private List<String> teachingLanguages;

    @Schema(description = "Rajaa tarjontaa toteutukselle määritellyn opetuskielen mukaan. Oletuksena palautetaan kaikilla opetuskielillä olevat tarjonnat.")
    private List<String> realisationTeachingLanguages;

    @Schema(description = "Rajaa tarjontaa tilojen mukaan. Oletuksena palautetaan vain aktiiviset.")
    private List<StudyStatus> statuses = Arrays.asList(StudyStatus.ACTIVE);

    @Schema(description = "Rajaa opintojaksojen toteutuksia niiden tilojen mukaan. Oletuksena palautetaan kaikki.")
    private List<StudyStatus> realisationStatuses;

    @Schema(description = "Rajaa tarjontaa koulutusalojen mukaan. Oletuksena palautetaan kaikilla koulutusaloilla olevat tarjonnat.")
    private List<MinEduGuidanceArea> minEduGuidanceAreas;

    @Schema(description = "Rajaa tarjontaa toteutukselle määriteltyjen koulutusalojen mukaan. Oletuksena palautetaan kaikilla koulutusaloilla olevat tarjonnat.")
    private List<MinEduGuidanceArea> realisationMinEduGuidanceAreas;

    /**
     * this is populated by StudiesService after we are sure what networks to actually use in the search in order to avoid re-fetching later.
     * check out StudiesNativeSearchQueryBuilder.
     */
    @JsonIgnore
    private List<NetworkEntity> actualNetworksUsedInSearch;

    /**
     * this is populated by StudiesService. used in terms aggregates.
     */
    @JsonIgnore
    private int organisationAmount;

    public Language getLanguage() {
        return language != null ? language : Language.FI;
    }

    public StudiesSearchElementType getType() {
        return type != null ? type : StudiesSearchElementType.ALL;
    }

    public StudiesSearchSortField getSortBy() {
        return sortBy != null ? sortBy : StudiesSearchSortField.NONE;
    }

    public Sort.Direction getSortDirection() {
        return sortDirection != null ? sortDirection : Sort.Direction.ASC;
    }

    public List<String> getTeachingLanguages() {
        return CollectionUtils.isEmpty(teachingLanguages) ?
            null : teachingLanguages.stream().map(tl -> tl.toLowerCase().trim()).collect(Collectors.toList());
    }

    public List<String> getRealisationTeachingLanguages() {
        return CollectionUtils.isEmpty(realisationTeachingLanguages) ?
            null : realisationTeachingLanguages.stream().map(rtl -> rtl.toLowerCase().trim()).collect(Collectors.toList());
    }

    public List<StudyStatus> getStatuses() {
        return statuses != null ? statuses : Arrays.asList(StudyStatus.ACTIVE);
    }

    @JsonIgnore
    public List<String> getActualNetworkIdsUsedInSearch() {
        return CollectionUtils.isEmpty(actualNetworksUsedInSearch) ? Collections.emptyList() : actualNetworksUsedInSearch.stream()
            .map(NetworkEntity::getId)
            .collect(Collectors.toList());
    }
}
