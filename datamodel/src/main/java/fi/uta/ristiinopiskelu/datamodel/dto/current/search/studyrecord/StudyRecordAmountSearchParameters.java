package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "StudyRecordAmountSearchParametersV9", title = "StudyRecordAmountSearchParametersV9")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StudyRecordAmountSearchParameters implements SearchParameters<StudyRecordEntity> {

    @Schema(description = "Verkoston tunniste")
    private String networkIdentifier;

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
}
