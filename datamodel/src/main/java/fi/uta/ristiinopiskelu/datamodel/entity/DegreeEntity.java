package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Document(indexName = "tutkinnot", createIndex = false)
public class DegreeEntity extends StudyElementEntity implements Serializable {
    private double duration;

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getDuration() {
        return this.duration;
    }

    public DegreeEntity() {
        setType(CompositeIdentifiedEntityType.DEGREE);
    }

    public DegreeEntity(double duration) {
        this();
        this.duration = duration;
    }

    public String toString() {
        return "Degree(duration=" + this.getDuration() + ")";
    }

}
