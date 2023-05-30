package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.code;

public class CodeSetKeyWithCodeCount {

    private String key;
    private Long count;

    public CodeSetKeyWithCodeCount(String key, Long count) {
        this.key = key;
        this.count = count;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
