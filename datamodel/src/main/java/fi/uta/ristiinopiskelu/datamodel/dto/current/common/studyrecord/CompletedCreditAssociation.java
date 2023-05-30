package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

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
}
