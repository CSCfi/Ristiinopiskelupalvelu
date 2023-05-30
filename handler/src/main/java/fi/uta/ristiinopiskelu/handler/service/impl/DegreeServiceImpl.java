package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.degree.DegreeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.degree.DegreeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.DegreeEntity;
import fi.uta.ristiinopiskelu.handler.exception.DeleteFailedException;
import fi.uta.ristiinopiskelu.handler.service.DegreeService;
import fi.uta.ristiinopiskelu.persistence.repository.DegreeRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyElementRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DegreeServiceImpl extends AbstractStudyElementService<DegreeWriteDTO, DegreeEntity, DegreeReadDTO> implements DegreeService {

    @Autowired
    private DegreeRepository degreeRepository;

    @Autowired
    private ModelMapper modelMapper;

    public DegreeServiceImpl() {
        super(DegreeWriteDTO.class, DegreeEntity.class, DegreeReadDTO.class, StudyElementType.DEGREE);
    }

    @Override
    protected StudyElementRepository<DegreeEntity> getRepository() {
        return degreeRepository;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public DegreeEntity delete(String studyElementId, String organizingOrganisationId, boolean deleteSubElements) throws DeleteFailedException {
        return this.deleteByStudyElementIdAndOrganizingOrganisationId(studyElementId, organizingOrganisationId);
    }
}
