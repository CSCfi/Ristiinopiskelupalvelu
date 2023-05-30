package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

public abstract class GenericEntity {

    @Id
    private String id;

    @Version
    private Long version;

    // Language fields for searching
    @JsonIgnore
    private String search_fi;
    @JsonIgnore
    private String search_en;
    @JsonIgnore
    private String search_sv;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public String getSearch_en() {
        return search_en;
    }

    public String getSearch_fi() {
        return search_fi;
    }

    public String getSearch_sv() {
        return search_sv;
    }

    public void setSearch_en(String search_en) {
        this.search_en = search_en;
    }

    public void setSearch_fi(String search_fi) {
        this.search_fi = search_fi;
    }

    public void setSearch_sv(String search_sv) {
        this.search_sv = search_sv;
    }

    public GenericEntity(String id){
        this.id = id;
    }
    public GenericEntity(){
    }
}
