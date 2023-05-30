package fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord;

import io.swagger.v3.oas.annotations.media.Schema;

public class CompletedCreditTarget {

    @Schema(description = "Rajaa tuloksia suorituskohteen tyypin mukaan")
    private CompletedCreditTargetType completedCreditTargetType;

    @Schema(description = "Rajaa tuloksia suorituskohteen tunnisteen mukaan")
    private String completedCreditTargetId;

    @Schema(description = "Rajaa tuloksia suorituskohteen tunnistekoodin mukaan")
    private String completedCreditTargetIdentifierCode;

    public CompletedCreditTargetType getCompletedCreditTargetType() {
        return completedCreditTargetType;
    }

    public void setCompletedCreditTargetType(CompletedCreditTargetType completedCreditTargetType) {
        this.completedCreditTargetType = completedCreditTargetType;
    }

    public String getCompletedCreditTargetId() {
        return completedCreditTargetId;
    }

    public void setCompletedCreditTargetId(String completedCreditTargetId) {
        this.completedCreditTargetId = completedCreditTargetId;
    }

    public String getCompletedCreditTargetIdentifierCode() {
        return completedCreditTargetIdentifierCode;
    }

    public void setCompletedCreditTargetIdentifierCode(String completedCreditTargetIdentifierCode) {
        this.completedCreditTargetIdentifierCode = completedCreditTargetIdentifierCode;
    }
}
