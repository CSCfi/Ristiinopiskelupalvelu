package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord.StudyRecordReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordAmountSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordAmountSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyrecord.StudyRecordWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.InvalidSearchParametersException;

public interface StudyRecordService extends Service<StudyRecordWriteDTO, StudyRecordEntity, StudyRecordReadDTO> {

    StudyRecordAmountSearchResults searchAmounts(String organisationId, StudyRecordAmountSearchParameters searchParams) throws FindFailedException, InvalidSearchParametersException;

    StudyRecordSearchResults search(String organisationId, StudyRecordSearchParameters searchParameters) throws FindFailedException, InvalidSearchParametersException;
}
