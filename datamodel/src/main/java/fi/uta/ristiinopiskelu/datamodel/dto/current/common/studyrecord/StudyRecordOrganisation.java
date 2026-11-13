package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.StudyRecordOrganisationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StudyRecordOrganisationV9", title = "StudyRecordOrganisationV9")
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

  public static StudyRecordOrganisation from(StudyRecordOrganisationEntity source) {
    if (source == null) {
      return null;
    }
    StudyRecordOrganisation target = new StudyRecordOrganisation();
    target.setOrganisationIdentifier(source.getOrganisationIdentifier());
    target.setOrganisationTkCode(source.getOrganisationTkCode());
    target.setOrganisationName(fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString.from(source.getOrganisationName()));
    target.setUnitName(fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString.from(source.getUnitName()));
    target.setNameDescription(fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString.from(source.getNameDescription()));
    target.setPostalAddress(Address.from(source.getPostalAddress()));
    target.setStreetAddress(Address.from(source.getStreetAddress()));
    target.setMunicipalityCode(source.getMunicipalityCode());
    target.setPhone(Phone.from(source.getPhone()));
    target.setUrl(source.getUrl());
    target.setPIC(source.getPIC());
    target.setErasmusCode(source.getErasmusCode());
    target.setHEI(source.getHEI());
    target.setErasmusCharterCode(source.getErasmusCharterCode());
    target.setErasmusOrganisationStatisticsCode(source.getErasmusOrganisationStatisticsCode());
    target.setErasmusOrganisationName(source.getErasmusOrganisationName());
    return target;
  }
}
