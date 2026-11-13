package fi.uta.ristiinopiskelu.handler;

import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EmbeddedElasticsearchInitializer implements BeforeAllCallback, AfterEachCallback, ExtensionContext.Store.CloseableResource {

    private static ElasticsearchContainer embeddedElastic = null;
    private final List<IndexHelper> indices = new ArrayList<>();

    public EmbeddedElasticsearchInitializer() {
        indices.add(new IndexHelper("opintokokonaisuudet_test","opintokokonaisuus", "opintokokonaisuudet"));
        indices.add(new IndexHelper("opintojaksot_test","opintojakso", "opintojaksot"));
        indices.add(new IndexHelper("toteutukset_test","toteutus", "toteutukset"));
        indices.add(new IndexHelper("koulut_test","koulu", "koulut"));
        indices.add(new IndexHelper("opintosuoritukset_test","opintosuoritus", "opintosuoritukset"));
        indices.add(new IndexHelper("tutkinnot_test","tutkinto", "tutkinnot"));
        indices.add(new IndexHelper("verkostot_test","verkosto", "verkostot"));
        indices.add(new IndexHelper("rekisteroinnit_test","rekisterointi", "rekisteroinnit"));
        indices.add(new IndexHelper("opiskelijat_test", "opiskelija", "opiskelijat"));
        indices.add(new IndexHelper("opintojaksot-history_test","opintojakso-history", "opintojaksot-history"));
        indices.add(new IndexHelper("opintokokonaisuudet-history_test","opintokokonaisuus-history", "opintokokonaisuudet-history"));
        indices.add(new IndexHelper("toteutukset-history_test","toteutus-history", "toteutukset-history"));
        indices.add(new IndexHelper("viestiskeemat_test", "viestiskeema", "viestiskeemat"));
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        if(embeddedElastic == null) {
            embeddedElastic = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch").withTag("9.3.2"));
            embeddedElastic.addEnv("cluster.name", "ripa-cluster");
            embeddedElastic.addEnv("xpack.security.enabled", "false");
            embeddedElastic.start();

            ElasticsearchTemplate template = SpringExtension.getApplicationContext(extensionContext).getBean(ElasticsearchTemplate.class);
            createIndices(template);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ElasticsearchTemplate template = SpringExtension.getApplicationContext(context).getBean(ElasticsearchTemplate.class);
        deleteAllAndRefresh(template);
    }

    @Override
    public void close() {
        embeddedElastic.stop();
    }

    private void createIndices(ElasticsearchTemplate template) {
        for(IndexHelper indexHelper : indices) {
            IndexOperations indexOperations = template.indexOps(IndexCoordinates.of(indexHelper.indexName));
            indexOperations.create(indexHelper.getSettings(), indexHelper.getMapper());
            indexOperations.alias(
                new AliasActions(
                    new AliasAction.Add(AliasActionParameters.builder().withIndices(indexHelper.indexName).withAliases(indexHelper.aliasName).build()
                    )
                )
            );
        }
    }

    private void deleteAllAndRefresh(ElasticsearchTemplate template) {
        for(IndexHelper indexHelper : indices) {
            DeleteByQueryRequest.Builder deleteByQueryRequest = new DeleteByQueryRequest.Builder()
                .index(indexHelper.indexName)
                .query(q -> q.matchAll(ma -> ma))
                .waitForCompletion(true)
                .refresh(true);

            template.execute(client -> client.deleteByQuery(deleteByQueryRequest.build()));

            IndexOperations indexOperations = template.indexOps(IndexCoordinates.of(indexHelper.indexName));
            indexOperations.refresh();
        }
    }

    static String getElasticHostAddress() {
        return embeddedElastic.getHttpHostAddress();
    }

    @Data
    private static class IndexHelper {
        private final String indexName;
        private final String aliasName;
        private final String typeName;
        private final Settings settings;
        private final Document mapper;

        public IndexHelper(String indexName, String typeName, String aliasName) {
            this.indexName = indexName;
            this.aliasName = aliasName;
            this.typeName = typeName;

            this.settings = parseSettings(aliasName);
            this.mapper = parseMapper(aliasName);
        }

        private Settings parseSettings(String aliasName) {
            Settings parsedSettings = null;

            try(InputStream settings = this.getClass().getResourceAsStream(String.format("/settings/%s.json", aliasName))) {
                if (settings != null && settings.available() > 0) {
                    parsedSettings = Settings.parse(new String(settings.readAllBytes()));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read settings files for index %s".formatted(aliasName), e);
            }

            if(parsedSettings == null) {
                parsedSettings = new Settings();
            }

            return parsedSettings;
        }

        private Document parseMapper(String aliasName) {
            Document parsedMapper = null;

            try(InputStream mapper = this.getClass().getResourceAsStream(String.format("/mappers/%s.json", aliasName))) {
                if (mapper != null && mapper.available() > 0) {
                    parsedMapper = Document.parse(new String(mapper.readAllBytes()));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read mapper files for index %s".formatted(aliasName), e);
            }

            if(parsedMapper == null) {
                throw new IllegalStateException("No mapper file parsed for index %s".formatted(aliasName));
            }

            return parsedMapper;
        }
    }
}
