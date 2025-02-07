package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

    public GenericEntity(String id){
        this.id = id;
    }
}
