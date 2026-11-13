package fi.uta.ristiinopiskelu.handler.helper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.List;

public class HistoryHelper {

    public static <T> List<T> queryHistoryIndex(ElasticsearchTemplate elasticsearchTemplate, String historyIndex, Class<T> type) {
        IndexOperations indexOperations = elasticsearchTemplate.indexOps(IndexCoordinates.of(historyIndex));
        indexOperations.refresh();

        NativeQuery searchQuery = new NativeQueryBuilder()
                .withQuery(q -> q.matchAll(ma -> ma))
                .withPageable(PageRequest.of(0, 1000))
                .build();

        try (SearchHitsIterator<T> searchHits = elasticsearchTemplate.searchForStream(
                searchQuery, type, IndexCoordinates.of(historyIndex))) {
            return searchHits.stream().map(SearchHit::getContent).toList();
        }
    }
}
