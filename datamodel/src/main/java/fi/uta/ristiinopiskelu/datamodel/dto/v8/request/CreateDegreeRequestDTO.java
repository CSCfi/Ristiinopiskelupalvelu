package fi.uta.ristiinopiskelu.datamodel.dto.v8.request;

public class CreateDegreeRequestDTO extends CreateStudyElementRequestDTO {

    private double duration;

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getDuration() {
        return this.duration;
    }

    public CreateDegreeRequestDTO() {
    }

    public CreateDegreeRequestDTO(double duration) {
        this.duration = duration;
    }

    public String toString() {
        return "Degree(duration=" + this.getDuration() + ")";
    }
}
