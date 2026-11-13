package fi.uta.ristiinopiskelu.persistence.repository.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;

import java.util.List;

final class ElasticsearchQueryUtils {

    static final int SEARCH_BATCH_SIZE = 1000;

    private ElasticsearchQueryUtils() {
    }

    static <T> List<T> search(ElasticsearchTemplate elasticsearchTemplate, NativeQuery query, Class<T> entityClass) {
        Pageable pageable = query.getPageable();

        if (pageable.isPaged()) {
            return elasticsearchTemplate.search(query, entityClass).stream()
                .map(SearchHit::getContent)
                .toList();
        }

        query.setReactiveBatchSize(SEARCH_BATCH_SIZE);

        try (SearchHitsIterator<T> searchHits = elasticsearchTemplate.searchForStream(query, entityClass)) {
            return searchHits.stream()
                .map(SearchHit::getContent)
                .toList();
        }
    }
}
