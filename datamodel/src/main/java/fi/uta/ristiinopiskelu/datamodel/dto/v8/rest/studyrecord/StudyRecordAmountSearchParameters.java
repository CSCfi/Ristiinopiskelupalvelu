package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.SearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCreditStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCreditTarget;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public class StudyRecordAmountSearchParameters implements SearchParameters<StudyRecord> {

    @Schema(description = "Määrittää haetaanko vastaanotettuja suorituksia (INCOMING) vai lähetettyjä suorituksia (OUTGOING)")
    private SearchDirection type;

    @Schema(description = "Rajaa tuloksia opetusinstituution mukaan")
    private String educationInstitution;

    @Schema(description = "Määrittää suorituskohteen nimen kielen (käytetään yhdessä 'name'-hakuehdon kanssa). Mahdolliset arvot: fi, sv, en. Oletusarvo: fi")
    private Language nameLanguage = Language.FI;

    @Schema(description = "Rajaa tuloksia suorituskohteen nimen mukaan")
    private String name;

    @Schema(description = "Rajaa tuloksia suorituskohteen tyypin, tunnisteen tai tunnistekoodin mukaan")
    private CompletedCreditTarget completedCreditTarget;

    @Schema(description = "Rajaa tuloksia suorituskohteen tilan mukaan")
    private CompletedCreditStatus status;

    @Schema(description = "Päivämäärä, milloin suoritus on aikaisintaan tehty")
    private LocalDate completionDateStart;

    @Schema(description = "Päivämäärä, milloin suoritus on viimeistään tehty.")
    private LocalDate completionDateEnd;

    public SearchDirection getType() {
        return type;
    }

    public void setType(SearchDirection type) {
        this.type = type;
    }

    public String getEducationInstitution() {
        return educationInstitution;
    }

    public void setEducationInstitution(String educationInstitution) {
        this.educationInstitution = educationInstitution;
    }

    public Language getNameLanguage() {
        return nameLanguage;
    }

    public void setNameLanguage(Language nameLanguage) {
        this.nameLanguage = nameLanguage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CompletedCreditTarget getCompletedCreditTarget() {
        return completedCreditTarget;
    }

    public void setCompletedCreditTarget(CompletedCreditTarget completedCreditTarget) {
        this.completedCreditTarget = completedCreditTarget;
    }

    public LocalDate getCompletionDateStart() {
        return completionDateStart;
    }

    public void setCompletionDateStart(LocalDate completionDateStart) {
        this.completionDateStart = completionDateStart;
    }

    public LocalDate getCompletionDateEnd() {
        return completionDateEnd;
    }

    public void setCompletionDateEnd(LocalDate completionDateEnd) {
        this.completionDateEnd = completionDateEnd;
    }

    public CompletedCreditStatus getStatus() {
        return status;
    }

    public void setStatus(CompletedCreditStatus status) {
        this.status = status;
    }
}
