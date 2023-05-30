package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public class StudyRecordAmountSearchParameters implements SearchParameters<StudyRecordEntity> {

    @Schema(description = "Lähettävän organisaation tilastokeskuskoodi")
    private String sendingOrganisation;

    @Schema(description = "Vasaanottavan organisaation tilastokeskuskoodi")
    private String receivingOrganisation;

    @Schema(description = "Päivämäärä, milloin suoritus on aikaisintaan tehty")
    private LocalDate completionDateStart;

    @Schema(description = "Päivämäärä, milloin suoritus on viimeistään tehty.")
    private LocalDate completionDateEnd;

    private StudyRecordGrouping groupBy;
    private StudyRecordDividing divideBy;

    private List<StudyRecordGroupingDates> groupByDates;

    public String getSendingOrganisation() {
        return sendingOrganisation;
    }

    public void setSendingOrganisation(String sendingOrganisation) {
        this.sendingOrganisation = sendingOrganisation;
    }

    public String getReceivingOrganisation() {
        return receivingOrganisation;
    }

    public void setReceivingOrganisation(String receivingOrganisation) {
        this.receivingOrganisation = receivingOrganisation;
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

    public StudyRecordGrouping getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(StudyRecordGrouping groupBy) {
        this.groupBy = groupBy;
    }

    public StudyRecordDividing getDivideBy() {
        return divideBy;
    }

    public void setDivideBy(StudyRecordDividing divideBy) {
        this.divideBy = divideBy;
    }

    public List<StudyRecordGroupingDates> getGroupByDates() {
        return groupByDates;
    }

    public void setGroupByDates(List<StudyRecordGroupingDates> groupByDates) {
        this.groupByDates = groupByDates;
    }
}
