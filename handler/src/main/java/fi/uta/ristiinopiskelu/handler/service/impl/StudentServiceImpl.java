package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.student.StudentReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.student.StudentWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.handler.service.result.GenericEntityModificationResult;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
public class StudentServiceImpl extends AbstractService<StudentWriteDTO, StudentEntity, StudentReadDTO> implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JsonMapper jsonMapper;

    public StudentServiceImpl() {
        super(StudentWriteDTO.class, StudentEntity.class, StudentReadDTO.class);
    }

    @Override
    protected ExtendedRepository<StudentEntity, String> getRepository() {
        return studentRepository;
    }

    @Override
    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Override
    public List<GenericEntityModificationResult> create(StudentEntity entity) throws CreateFailedException {
        return super.create(entity).stream()
                .map(GenericEntityModificationResult.class::cast)
                .toList();
    }

    @Override
    public List<StudentEntity> findByOidOrPersonId(String oid, String personId, Pageable pageable) throws FindFailedException {
       try {
            return studentRepository.findByOidOrPersonId(oid, personId, pageable);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), oid, e);
        }
    }
}
