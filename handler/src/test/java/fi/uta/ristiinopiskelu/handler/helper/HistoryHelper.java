package fi.uta.ristiinopiskelu.handler.helper;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.List;

public class HistoryHelper {

    public static <T> List<T> queryHistoryIndex(ElasticsearchTemplate elasticsearchTemplate, String historyIndex, Class<T> type) {
        IndexOperations indexOperations = elasticsearchTemplate.indexOps(IndexCoordinates.of(historyIndex));
        indexOperations.refresh();

        NativeQuery searchQuery = new NativeQueryBuilder()
                .withQuery(q -> q.matchAll(ma -> ma))
                .withPageable(Pageable.unpaged())
                .build();

        return elasticsearchTemplate.search(searchQuery, type, IndexCoordinates.of(historyIndex))
            .get().map(SearchHit::getContent).toList();
    }
}
