package fi.uta.ristiinopiskelu.datamodel.dto.current.common.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Person;

import java.util.Objects;

public class Student extends Person {

    private String oid;
    private String personId;
    private String homeStudentNumber;
    private String hostStudentNumber;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getHomeStudentNumber() {
        return homeStudentNumber;
    }

    public void setHomeStudentNumber(String homeStudentNumber) {
        this.homeStudentNumber = homeStudentNumber;
    }

    public String getHostStudentNumber() {
        return hostStudentNumber;
    }

    public void setHostStudentNumber(String hostStudentNumber) {
        this.hostStudentNumber = hostStudentNumber;
    }

    public Student() {

    }

    public Student(ExtendedStudent student) {
        this.setOid(student.getOid());
        this.setPersonId(student.getPersonId());
        this.setHomeStudentNumber(student.getHomeStudentNumber());
        this.setHostStudentNumber(student.getHostStudentNumber());
        this.setHomeEppn(student.getHomeEppn());
        this.setHostEppn(student.getHostEppn());
        this.setFirstNames(student.getFirstNames());
        this.setSurName(student.getSurName());
        this.setGivenName(student.getGivenName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student student = (Student) o;
        return Objects.equals(oid, student.oid) &&
                Objects.equals(personId, student.personId) &&
                Objects.equals(homeStudentNumber, student.homeStudentNumber) &&
                Objects.equals(hostStudentNumber, student.hostStudentNumber);
    }

    @Override
    public int hashCode() {

        return Objects.hash(oid, personId, homeStudentNumber, hostStudentNumber);
    }
}
