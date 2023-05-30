package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.handler.exception.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface Service<W, T extends GenericEntity, R> {

    T create(T entity) throws CreateFailedException;

    List<T> createAll(List<T> entities) throws CreateFailedException;

    T update(T entity) throws UpdateFailedException;

    T deleteById(String id) throws DeleteFailedException;

    Optional<T> findById(String id) throws FindFailedException;

    List<T> findByIds(List<String> ids) throws FindFailedException;

    List<T> findAll(Pageable pageable) throws FindFailedException;

    SearchResults<R> search(String organisationId, SearchParameters<T> searchParameters) throws FindFailedException, InvalidSearchParametersException;

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
