package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditAssociationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CompletedCreditAssociationV9", title = "CompletedCreditAssociationV9")
public class CompletedCreditAssociation {
  private CompletedCreditAssociationType completedCreditAssociationType;
  private String completedCreditAssociationId;
  private String completedCreditAssociationIdentifierCode;

  public CompletedCreditAssociationType getCompletedCreditAssociationType() {
    return completedCreditAssociationType;
  }

  public void setCompletedCreditAssociationType(CompletedCreditAssociationType completedCreditAssociationType) {
    this.completedCreditAssociationType = completedCreditAssociationType;
  }

  public String getCompletedCreditAssociationId() {
    return completedCreditAssociationId;
  }

  public void setCompletedCreditAssociationId(String completedCreditAssociationId) {
    this.completedCreditAssociationId = completedCreditAssociationId;
  }

  public String getCompletedCreditAssociationIdentifierCode() {
    return completedCreditAssociationIdentifierCode;
  }

  public void setCompletedCreditAssociationIdentifierCode(String completedCreditAssociationIdentifierCode) {
    this.completedCreditAssociationIdentifierCode = completedCreditAssociationIdentifierCode;
  }

  public static CompletedCreditAssociation from(CompletedCreditAssociationEntity source) {
    if (source == null) {
      return null;
    }

    CompletedCreditAssociation target = new CompletedCreditAssociation();
    target.setCompletedCreditAssociationType(source.getCompletedCreditAssociationType() == null ? null
      : CompletedCreditAssociationType.valueOf(source.getCompletedCreditAssociationType().name()));
    target.setCompletedCreditAssociationId(source.getCompletedCreditAssociationId());
    target.setCompletedCreditAssociationIdentifierCode(source.getCompletedCreditAssociationIdentifierCode());
    return target;
  }

  public static List<CompletedCreditAssociation> from(List<CompletedCreditAssociationEntity> source) {
    if (source == null) {
      return null;
    }
    return source.stream().map(CompletedCreditAssociation::from).toList();
  }
}
