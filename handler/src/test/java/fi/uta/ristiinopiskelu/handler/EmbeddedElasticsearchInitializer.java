package fi.uta.ristiinopiskelu.handler;

import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class EmbeddedElasticsearchInitializer implements AfterEachCallback, BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedElasticsearchInitializer.class);

    private static ElasticsearchContainer embeddedElastic = null;

    private final List<IndexHelper> indices = new ArrayList<>();

    public class IndexHelper {
        public String indexName;
        public String aliasName;
        public String typeName;

        public IndexHelper(String indexName, String typeName, String aliasName) {
            this.indexName = indexName;
            this.aliasName = aliasName;
            this.typeName = typeName;
        }
    }

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
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if(embeddedElastic == null) {
            embeddedElastic = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch").withTag("8.10.4"));
            embeddedElastic.addEnv("cluster.name", "ripa-cluster");
            embeddedElastic.addEnv("xpack.security.enabled", "false");
            embeddedElastic.addEnv("indices.id_field_data.enabled", "true");
            embeddedElastic.start();

            ElasticsearchTemplate template = SpringExtension.getApplicationContext(extensionContext).getBean(ElasticsearchTemplate.class);
            createIndices(template);
        }
    }

    @Override
    public void close() throws Throwable {
        embeddedElastic.stop();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        ElasticsearchTemplate template = SpringExtension.getApplicationContext(context).getBean(ElasticsearchTemplate.class);

        for(IndexHelper indexHelper : indices) {
            deleteAllAndRefresh(template, indexHelper);
        }
    }

    private void createIndices(ElasticsearchTemplate template) {
        for(IndexHelper indexHelper : indices) {
            IndexOperations indexOperations = template.indexOps(IndexCoordinates.of(indexHelper.indexName));

            Settings parsedSettings = null;
            Document parsedMapper = null;

            try(InputStream settings = this.getClass().getResourceAsStream(String.format("/settings/%s.json", indexHelper.aliasName));
                InputStream mapper = this.getClass().getResourceAsStream(String.format("/mappers/%s.json", indexHelper.aliasName))) {
                if (settings != null && settings.available() > 0) {
                    parsedSettings = Settings.parse(new String(settings.readAllBytes()));
                }

                if (mapper != null && mapper.available() > 0) {
                    parsedMapper = Document.parse(new String(mapper.readAllBytes()));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read settings/mapper files for index " + indexHelper.aliasName, e);
            }

            if(parsedMapper == null) {
                throw new IllegalStateException("No mapper file parsed for index " + indexHelper.aliasName);
            }

            if(parsedSettings == null) {
                parsedSettings = new Settings();
            }

            indexOperations.create(parsedSettings, parsedMapper);
            indexOperations.alias(
                new AliasActions(
                    new AliasAction.Add(AliasActionParameters.builder().withIndices(indexHelper.indexName).withAliases(indexHelper.aliasName).build()
                    )
                )
            );
        }
    }

    static String getElasticHostAddress() {
        return embeddedElastic.getHttpHostAddress();
    }

    private void deleteAllAndRefresh(ElasticsearchTemplate template, IndexHelper indexHelper) {
        DeleteByQueryRequest.Builder deleteByQueryRequest = new DeleteByQueryRequest.Builder()
            .index(indexHelper.indexName)
            .query(q -> q.matchAll(ma -> ma));

        template.execute(client -> client.deleteByQuery(deleteByQueryRequest.build()));

        IndexOperations indexOperations = template.indexOps(IndexCoordinates.of(indexHelper.indexName));
        indexOperations.refresh();
    }
}
