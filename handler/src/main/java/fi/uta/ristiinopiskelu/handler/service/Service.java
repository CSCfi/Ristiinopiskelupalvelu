package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.DeleteFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.InvalidSearchParametersException;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.service.result.EntityModificationResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface Service<W, T extends GenericEntity, R> {

    List<? extends EntityModificationResult<?>> create(T entity) throws CreateFailedException;

    List<? extends EntityModificationResult<?>> createAll(List<T> entities) throws CreateFailedException;

    List<? extends EntityModificationResult<?>> update(T entity) throws UpdateFailedException;

    List<? extends EntityModificationResult<?>> deleteById(String id) throws DeleteFailedException;

    Optional<T> findById(String id) throws FindFailedException;

    List<T> findByIds(List<String> ids) throws FindFailedException;

    List<T> findAll(Pageable pageable) throws FindFailedException;

    SearchResults<R> search(String organisationId, SearchParameters<T> searchParameters) throws FindFailedException, InvalidSearchParametersException;

    <A> A copy(A original, Class<? extends A> type);

    R toReadDTO(T entity);

    /**
     * Fills the object's missing values from the entity
     * @param target
     * @param source
     * @return
     */
    <A> A fillMissingValues(A target, T source);

    <A> A fillMissingValuesById(A object, String id);
}
