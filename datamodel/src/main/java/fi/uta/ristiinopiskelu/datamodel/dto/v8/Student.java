package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class Student extends Person {

    private String oid;
    private String personId;
    private String homeStudentNumber;
    private String hostStudentNumber;

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

    public Student(String homeStudentNumber, String hostStudentNumber) {
        this.homeStudentNumber = homeStudentNumber;
        this.hostStudentNumber = hostStudentNumber;
    }

    public Student(String homeEppn, String hostEppn, String firstNames, String surName, String givenName, String oid,
                   String personId, String homeStudentNumber, String hostStudentNumber) {
        super(homeEppn, hostEppn, firstNames, surName, givenName);
        this.oid = oid;
        this.personId = personId;
        this.homeStudentNumber = homeStudentNumber;
        this.hostStudentNumber = hostStudentNumber;
    }
}
