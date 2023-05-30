package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;

public class StudyRecordOrganisation extends Organisation {

  private Integer PIC;
  private String erasmusCode;
  private String HEI;
  private String erasmusCharterCode;
  private Integer erasmusOrganisationStatisticsCode;
  private String erasmusOrganisationName;

  /**
   * Erasmus, e.g. 934620221
   * @return
   */
  public Integer getPIC() {
    return PIC;
  }

  public void setPIC(Integer PIC) {
    this.PIC = PIC;
  }

  /**
   * esim. (SF KUOPIO08)
   * @return
   */
  public String getErasmusCode() {
    return erasmusCode;
  }

  public void setErasmusCode(String erasmusCode) {
    this.erasmusCode = erasmusCode;
  }

  /**
   * esim. SAVONIA-AMMATTIKORKEAKOULU OY
    * @return
   */
  public String getHEI() {
    return HEI;
  }

  public void setHEI(String HEI) {
    this.HEI = HEI;
  }

  /**
   * esim. 9571-EPP-1-2014-1-FI-EPPKA3-ECHE
   * @return
   */
  public String getErasmusCharterCode() {
    return erasmusCharterCode;
  }

  public void setErasmusCharterCode(String erasmusCharterCode) {
    this.erasmusCharterCode = erasmusCharterCode;
  }

  /**
   * Erasmus, esim. 02537
   * @return
   */
  public Integer getErasmusOrganisationStatisticsCode() {
    return erasmusOrganisationStatisticsCode;
  }

  public void setErasmusOrganisationStatisticsCode(Integer erasmusOrganisationStatisticsCode) {
    this.erasmusOrganisationStatisticsCode = erasmusOrganisationStatisticsCode;
  }

  /**
   * Erasmus, esim. SAVONIA
   * @return
   */
  public String getErasmusOrganisationName() {
    return erasmusOrganisationName;
  }

  public void setErasmusOrganisationName(String erasmusOrganisationName) {
    this.erasmusOrganisationName = erasmusOrganisationName;
  }
}
