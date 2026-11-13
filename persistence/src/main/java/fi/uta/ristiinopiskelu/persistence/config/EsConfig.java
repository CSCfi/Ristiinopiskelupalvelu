package fi.uta.ristiinopiskelu.persistence.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import fi.uta.ristiinopiskelu.datamodel.converter.current.RistiinopiskeluEnumConverters;
import fi.uta.ristiinopiskelu.persistence.aspect.UncategorizedElasticsearchExceptionAspect;
import fi.uta.ristiinopiskelu.persistence.repository.impl.ExtendedRepositoryImpl;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "fi.uta.ristiinopiskelu.persistence.repository", repositoryBaseClass = ExtendedRepositoryImpl.class)
@ComponentScan(basePackages = "fi.uta.ristiinopiskelu.persistence.repository.impl")
public class EsConfig extends ElasticsearchConfiguration {

    @Value("${general.elasticsearch.hosts}")
    private String hosts;

    @Value("${general.elasticsearch.clusterName}")
    private String clusterName;

    @Value("${general.elasticsearch.username}")
    private String username;

    @Value("${general.elasticsearch.password}")
    private String password;

    @Value("${general.elasticsearch.caCertificatePath}")
    private String caCertificatePath;

    @Override
    public JsonpMapper jsonpMapper() {
        return new Jackson3JsonpMapper(jsonMapper(null));
    }

    @Override
    protected RefreshPolicy refreshPolicy() {
        return RefreshPolicy.IMMEDIATE;
    }

    @Override
    protected boolean writeTypeHints() {
        return false;
    }

    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(RistiinopiskeluEnumConverters.getConverters());
    }

    @Override
    public ClientConfiguration clientConfiguration() {
        Assert.hasText(hosts, "ElasticSearch hosts cannot be empty");

        boolean haveCredentials = StringUtils.hasText(username) && StringUtils.hasText(password);

        ClientConfiguration clientConfiguration;

        if(haveCredentials) {
            Assert.hasText(caCertificatePath, "ElasticSearch CA certificate path cannot be empty");

            clientConfiguration = ClientConfiguration.builder()
                .connectedTo(StringUtils.commaDelimitedListToStringArray(hosts.trim()))
                .usingSsl(getSSLContext())
                .withBasicAuth(username, password)
                .build();
        } else {
            clientConfiguration = ClientConfiguration.builder()
                .connectedTo(StringUtils.commaDelimitedListToStringArray(hosts.trim()))
                .build();
        }

        return clientConfiguration;
    }

    @Override
    public ElasticsearchTemplate elasticsearchOperations(ElasticsearchConverter elasticsearchConverter, ElasticsearchClient elasticsearchClient) {
        return (ElasticsearchTemplate) super.elasticsearchOperations(elasticsearchConverter, elasticsearchClient);
    }

    // aspect for wrapping all UncategorizedElasticsearchExceptions thrown from repositories with an implementation that produces more verbose exception messages
    @Bean
    public UncategorizedElasticsearchExceptionAspect uncategorizedElasticsearchExceptionAspect() {
        return new UncategorizedElasticsearchExceptionAspect();
    }

    @Primary
    @Bean("jacksonJsonMapper")
    public JsonMapper jsonMapper(List<JacksonModule> modules) {
        SimpleModule sanitizerModule = new SimpleModule();
        sanitizerModule.addDeserializer(String.class, new HtmlSanitizingStringDeserializer());

        SimpleModule offsetDateTimeConverterModule = new SimpleModule();
        offsetDateTimeConverterModule.addSerializer(OffsetDateTime.class, new ValueSerializer<>() {
            @Override
            public void serialize(OffsetDateTime offsetDateTime, tools.jackson.core.JsonGenerator jsonGenerator, SerializationContext ctxt) throws JacksonException {
                jsonGenerator.writeString(DateUtils.getFormatter().format(offsetDateTime));
            }
        });

        JsonMapper.Builder builder = JsonMapper.builder();

        if(!CollectionUtils.isEmpty(modules)) {
            modules.forEach(builder::addModule);
        }

        return builder
            .addModule(sanitizerModule)
            .addModule(offsetDateTimeConverterModule)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .defaultDateFormat(new SimpleDateFormat(DateUtils.getPattern()))
            .build();
    }

    private SSLContext getSSLContext() {
        try(InputStream caCertStream = new FileInputStream(caCertificatePath)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(caCertStream);

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null);
            ks.setCertificateEntry("caCert", caCert);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(ks);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagerFactory.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SSLContext", e);
        }
    }
}
