package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditTargetEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CompletedCreditTargetV9", title = "CopmletedCreditTargetV9")
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

    public static CompletedCreditTarget from(CompletedCreditTargetEntity source) {
        if (source == null) {
            return null;
        }
        CompletedCreditTarget target = new CompletedCreditTarget();
        target.setCompletedCreditTargetType(source.getCompletedCreditTargetType() == null ? null
            : CompletedCreditTargetType.valueOf(source.getCompletedCreditTargetType().name()));
        target.setCompletedCreditTargetId(source.getCompletedCreditTargetId());
        target.setCompletedCreditTargetIdentifierCode(source.getCompletedCreditTargetIdentifierCode());
        return target;
    }
}
