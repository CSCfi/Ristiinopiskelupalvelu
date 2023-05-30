package fi.uta.ristiinopiskelu.handler.helper;

import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class HistoryHelper {
    public static <T> List<T> queryHistoryIndex(ElasticsearchRestTemplate elasticsearchTemplate, String historyIndex, Class<T> type) {
        IndexOperations indexOperations = elasticsearchTemplate.indexOps(IndexCoordinates.of(historyIndex));
        indexOperations.refresh();

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(new MatchAllQueryBuilder())
            .build();

        return elasticsearchTemplate.search(searchQuery, type, IndexCoordinates.of(historyIndex))
            .get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
