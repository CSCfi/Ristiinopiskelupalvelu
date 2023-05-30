package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

public class StudyElementOrganisation extends Organisation {

  private String parentId;
  private String organisationType;

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getOrganisationType() {
    return organisationType;
  }

  public void setOrganisationType(String organisationType) {
    this.organisationType = organisationType;
  }
}
