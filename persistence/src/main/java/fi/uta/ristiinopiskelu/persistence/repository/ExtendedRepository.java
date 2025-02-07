package fi.uta.ristiinopiskelu.persistence.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface ExtendedRepository<T, ID extends Serializable>
        extends ElasticsearchRepository<T, ID> {

    T update(T instance);

    T create(T instance);

    T create(T instance, IndexQuery.OpType opType);

    List<T> search(Query query, Pageable pageable);

    List<T> search(Query query);
}
