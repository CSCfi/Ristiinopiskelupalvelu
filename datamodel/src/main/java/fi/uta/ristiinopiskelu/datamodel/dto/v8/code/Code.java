package fi.uta.ristiinopiskelu.datamodel.dto.v8.code;

import java.time.LocalDate;
import java.util.List;

public class Code {
    private String codeUri;
    private String resourceUri;
    private CodeSet codeSet;
    private String key;
    private Integer codeVersion;
    private LocalDate updateDate;
    private LocalDate validityStartDate;
    private LocalDate validityEndDate;
    private String status;
    private List<CodeValue> codeValues;

    public String getCodeUri() {
        return codeUri;
    }

    public void setCodeUri(String codeUri) {
        this.codeUri = codeUri;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public CodeSet getCodeSet() {
        return codeSet;
    }

    public void setCodeSet(CodeSet codeSet) {
        this.codeSet = codeSet;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getCodeVersion() {
        return codeVersion;
    }

    public void setCodeVersion(Integer codeVersion) {
        this.codeVersion = codeVersion;
    }

    public LocalDate getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDate updateDate) {
        this.updateDate = updateDate;
    }

    public LocalDate getValidityStartDate() {
        return validityStartDate;
    }

    public void setValidityStartDate(LocalDate validityStartDate) {
        this.validityStartDate = validityStartDate;
    }

    public LocalDate getValidityEndDate() {
        return validityEndDate;
    }

    public void setValidityEndDate(LocalDate validityEndDate) {
        this.validityEndDate = validityEndDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CodeValue> getCodeValues() {
        return codeValues;
    }

    public void setCodeValues(List<CodeValue> codeValues) {
        this.codeValues = codeValues;
    }

    public String toString() {
        return "Code(codeUri=" + this.getCodeUri() + ")";
    }

}
