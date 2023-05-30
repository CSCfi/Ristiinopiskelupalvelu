package fi.uta.ristiinopiskelu.handler.controller.v9.converter.dto;

import com.opencsv.bean.CsvBindByName;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord.StudyRecordReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordStudentReadDTO;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudyRecordCsvDTO {

    @CsvBindByName(column = "ID")
    private String id;

    @CsvBindByName(column = "SENDING_ORGANISATION")
    private String sendingOrganisation;

    @CsvBindByName(column = "RECEIVING_ORGANISATION")
    private String receivingOrganisation;

    @CsvBindByName(column = "COMPLETED_CREDIT_IDENTIFIER")
    private String completedCreditIdentifier;

    @CsvBindByName(column = "COMPLETED_CREDIT_NAME_FI")
    private String completedCreditNameFi;

    @CsvBindByName(column = "COMPLETED_CREDIT_NAME_EN")
    private String completedCreditNameEn;

    @CsvBindByName(column = "COMPLETED_CREDIT_NAME_SV")
    private String completedCerditNameSv;

    @CsvBindByName(column = "COMPLETED_CREDIT_TARGET_TYPE")
    private String completedCreditTargetType;

    @CsvBindByName(column = "COMPLETED_CREDIT_TARGET_ID")
    private String completedCreditTargetId;

    @CsvBindByName(column = "COMPLETED_CREDIT_TARGET_IDENTIFIER_CODE")
    private String completedCreditTargetIdentifierCode;

    @CsvBindByName(column = "GRADING_SCALE")
    private int gradingScale;

    @CsvBindByName(column = "GRADE_CODE")
    private String gradeCode;

    @CsvBindByName(column = "COMPLETION_DATE")
    private LocalDate completionDate;

    @CsvBindByName(column = "SCOPE")
    private Double scope;

    @CsvBindByName(column = "STUDENT_OID")
    private String studentOid;

    @CsvBindByName(column = "STUDENT_FIRST_NAMES")
    private String studentFirstNames;

    @CsvBindByName(column = "STUDENT_SURNAME")
    private String studentSurName;

    @CsvBindByName(column = "STUDENT_GIVENNAME")
    private String studentGivenName;

    @CsvBindByName(column = "ORGANISATION_RESPONSIBLE_FOR_COMPLETION_TK_CODE")
    private String organisationResponsibleForCompletionTkCode;

    @CsvBindByName(column = "MIN_EDU_GUIDANCE_AREA")
    private int minEduGuidanceArea;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSendingOrganisation() {
        return sendingOrganisation;
    }

    public void setSendingOrganisation(String sendingOrganisation) {
        this.sendingOrganisation = sendingOrganisation;
    }

    public String getReceivingOrganisation() {
        return receivingOrganisation;
    }

    public void setReceivingOrganisation(String receivingOrganisation) {
        this.receivingOrganisation = receivingOrganisation;
    }

    public String getCompletedCreditIdentifier() {
        return completedCreditIdentifier;
    }

    public void setCompletedCreditIdentifier(String completedCreditIdentifier) {
        this.completedCreditIdentifier = completedCreditIdentifier;
    }

    public String getCompletedCreditNameFi() {
        return completedCreditNameFi;
    }

    public void setCompletedCreditNameFi(String completedCreditNameFi) {
        this.completedCreditNameFi = completedCreditNameFi;
    }

    public String getCompletedCreditNameEn() {
        return completedCreditNameEn;
    }

    public void setCompletedCreditNameEn(String completedCreditNameEn) {
        this.completedCreditNameEn = completedCreditNameEn;
    }

    public String getCompletedCerditNameSv() {
        return completedCerditNameSv;
    }

    public void setCompletedCerditNameSv(String completedCerditNameSv) {
        this.completedCerditNameSv = completedCerditNameSv;
    }

    public String getCompletedCreditTargetType() {
        return completedCreditTargetType;
    }

    public void setCompletedCreditTargetType(String completedCreditTargetType) {
        this.completedCreditTargetType = completedCreditTargetType;
    }

    public String getCompletedCreditTargetId() {
        return completedCreditTargetId;
    }

    public void setCompletedCreditTargetId(String completedCreditTargetId) {
        this.completedCreditTargetId = completedCreditTargetId;
    }

    public String getCompletedCreditTargetIdentifierCode() {
        return completedCreditTargetIdentifierCode;
    }

    public void setCompletedCreditTargetIdentifierCode(String completedCreditTargetIdentifierCode) {
        this.completedCreditTargetIdentifierCode = completedCreditTargetIdentifierCode;
    }

    public int getGradingScale() {
        return gradingScale;
    }

    public void setGradingScale(int gradingScale) {
        this.gradingScale = gradingScale;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public void setGradeCode(String gradeCode) {
        this.gradeCode = gradeCode;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public Double getScope() {
        return scope;
    }

    public void setScope(Double scope) {
        this.scope = scope;
    }

    public String getStudentOid() {
        return studentOid;
    }

    public void setStudentOid(String studentOid) {
        this.studentOid = studentOid;
    }

    public String getStudentFirstNames() {
        return studentFirstNames;
    }

    public void setStudentFirstNames(String studentFirstNames) {
        this.studentFirstNames = studentFirstNames;
    }

    public String getStudentSurName() {
        return studentSurName;
    }

    public void setStudentSurName(String studentSurName) {
        this.studentSurName = studentSurName;
    }

    public String getStudentGivenName() {
        return studentGivenName;
    }

    public void setStudentGivenName(String studentGivenName) {
        this.studentGivenName = studentGivenName;
    }

    public String getOrganisationResponsibleForCompletionTkCode() {
        return organisationResponsibleForCompletionTkCode;
    }

    public void setOrganisationResponsibleForCompletionTkCode(String organisationResponsibleForCompletionTkCode) {
        this.organisationResponsibleForCompletionTkCode = organisationResponsibleForCompletionTkCode;
    }

    public int getMinEduGuidanceArea() {
        return minEduGuidanceArea;
    }

    public void setMinEduGuidanceArea(int minEduGuidanceArea) {
        this.minEduGuidanceArea = minEduGuidanceArea;
    }

    public static List<StudyRecordCsvDTO> from(StudyRecordReadDTO studyRecordReadDTO) {
        List<StudyRecordCsvDTO> dtos = new ArrayList<>();
        
        if(!CollectionUtils.isEmpty(studyRecordReadDTO.getCompletedCredits())) {
            for(CompletedCredit completedCredit : studyRecordReadDTO.getCompletedCredits()) {
                StudyRecordCsvDTO dto = new StudyRecordCsvDTO();
                dto.setId(studyRecordReadDTO.getId());
                dto.setCompletedCreditIdentifier(completedCredit.getCompletedCreditIdentifier());
                dto.setCompletionDate(completedCredit.getCompletionDate());
                dto.setReceivingOrganisation(studyRecordReadDTO.getReceivingOrganisation());
                dto.setSendingOrganisation(studyRecordReadDTO.getSendingOrganisation());
                dto.setScope(completedCredit.getScope());

                MinEduGuidanceArea minEduGuidanceArea = completedCredit.getMinEduGuidanceArea();
                if(minEduGuidanceArea != null) {
                    dto.setMinEduGuidanceArea(minEduGuidanceArea.getCode());
                }

                LocalisedString completedCreditName = completedCredit.getCompletedCreditName();
                if(completedCreditName != null) {
                    dto.setCompletedCreditNameFi(completedCreditName.getValue(Language.FI));
                    dto.setCompletedCerditNameSv(completedCreditName.getValue(Language.SV));
                    dto.setCompletedCreditNameEn(completedCreditName.getValue(Language.EN));
                }

                CompletedCreditAssessment assessment = completedCredit.getAssessment();
                if(assessment != null) {
                    GradingScale scale = assessment.getGradingScale();
                    Grade grade = assessment.getGrade();

                    if(scale != null) {
                        ScaleValue scaleValue = scale.getScale();
                        if(scaleValue != null) {
                            dto.setGradingScale(scaleValue.getCode());
                        }
                    }

                    if(grade != null) {
                        GradeCode gradeCode = grade.getCode();
                        if(gradeCode != null) {
                            dto.setGradeCode(gradeCode.getCode());
                        }
                    }
                }

                CompletedCreditTarget target = completedCredit.getCompletedCreditTarget();
                if(target != null) {
                    dto.setCompletedCreditTargetId(target.getCompletedCreditTargetId());
                    dto.setCompletedCreditTargetIdentifierCode(target.getCompletedCreditTargetIdentifierCode());

                    CompletedCreditTargetType targetType = target.getCompletedCreditTargetType();
                    if(targetType != null) {
                        dto.setCompletedCreditTargetType(targetType.name());
                    }
                }

                StudyRecordOrganisation studyRecordOrganisation = completedCredit.getOrganisationResponsibleForCompletion();
                if(studyRecordOrganisation != null) {
                    dto.setOrganisationResponsibleForCompletionTkCode(studyRecordOrganisation.getOrganisationTkCode());
                }

                StudyRecordStudentReadDTO student = studyRecordReadDTO.getStudent();
                if(student != null) {
                    dto.setStudentFirstNames(student.getFirstNames());
                    dto.setStudentOid(student.getOid());
                    dto.setStudentSurName(student.getSurName());
                    dto.setStudentGivenName(student.getGivenName());
                }

                dtos.add(dto);
            }
        }

        return dtos;
    }
}
