package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.Student;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.student.StudentReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.student.StudentSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.student.StudentSearchResult;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.student.StudentSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.student.StudentWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RegistrationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl extends AbstractService<StudentWriteDTO, StudentEntity, StudentReadDTO> implements StudentService {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private ModelMapper modelMapper;

    public StudentServiceImpl() {
        super(StudentWriteDTO.class, StudentEntity.class, StudentReadDTO.class);
    }

    @Override
    protected ExtendedRepository<StudentEntity, String> getRepository() {
        return studentRepository;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public List<StudentEntity> findByOidOrPersonIdOrderByTimestampDesc(String oid, String personId) throws FindFailedException {
       try {
            return studentRepository.findByOidOrPersonIdOrderByTimestampDesc(oid, personId);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), oid, e);
        }
    }

    @Override
    public StudentSearchResults search(String organisationId, StudentSearchParameters searchParameters) throws FindFailedException {

        Assert.notNull(searchParameters, "Search parameters cannot be null");

        List<String> networkIdentifiers = searchParameters.getNetworkIdentifiers();

        // filter out all but the actually valid network identifiers from the search params
        List<NetworkEntity> validSearchNetworks = networkRepository.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged())
                .stream().filter(network -> !CollectionUtils.isEmpty(networkIdentifiers) && networkIdentifiers.contains(network.getId())).collect(Collectors.toList());

        // now find out which networks this organisation is actually a coordinator in and then get all organisation ids from those networks
        List<String> organisationIds = validSearchNetworks
                .stream().filter(network -> network.getOrganisations().stream().anyMatch(org -> org.getOrganisationTkCode().equals(organisationId) && org.getIsCoordinator()))
                .flatMap(network -> network.getOrganisations().stream())
                .map(NetworkOrganisation::getOrganisationTkCode)
                .collect(Collectors.toList());

        // not a coordinator in any network. limit to own.
        if(CollectionUtils.isEmpty(organisationIds)) {
            organisationIds.add(organisationId);
        }

        try {
            List<RegistrationEntity> registrations = registrationRepository.findAllByParams(searchParameters.getStudentOid(), searchParameters.getStudentPersonId(),
                    searchParameters.getStudentHomeEppn(), organisationIds, searchParameters.getSendDateTimeStart(), searchParameters.getSendDateTimeEnd(),
                    searchParameters.getRegistrationStatus(), searchParameters.getPageRequest());

            // distinct students
            List<Student> students = registrations.stream().map(reg -> new Student(reg.getStudent())).distinct().collect(Collectors.toList());

            List<StudentSearchResult> results = students.stream()
                    .map(student -> new StudentSearchResult(student,
                            registrations.stream().filter(reg -> new Student(reg.getStudent()).equals(student) && reg.getStudent().getHostStudyRight() != null)
                                    .map(e -> e.getStudent().getHostStudyRight()).collect(Collectors.toList()),
                            registrations.stream().filter(reg -> new Student(reg.getStudent()).equals(student) && reg.getStudent().getHomeStudyRight() != null)
                                    .map(e -> e.getStudent().getHomeStudyRight()).collect(Collectors.toList())))
                    .collect(Collectors.toList());

            return new StudentSearchResults(results);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }
}
