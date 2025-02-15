package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.student.StudentReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.student.StudentSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.student.StudentSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.student.StudentWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.result.GenericEntityModificationResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService extends Service<StudentWriteDTO, StudentEntity, StudentReadDTO> {

    List<GenericEntityModificationResult> create(StudentEntity entity) throws CreateFailedException;

    List<StudentEntity> findByOidOrPersonId(String oid, String personId, Pageable pageable) throws FindFailedException;

    StudentSearchResults search(String organisationId, StudentSearchParameters searchParameters) throws FindFailedException;
}
