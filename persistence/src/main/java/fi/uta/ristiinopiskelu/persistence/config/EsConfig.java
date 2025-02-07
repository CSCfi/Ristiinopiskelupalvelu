package fi.uta.ristiinopiskelu.persistence.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.converter.current.RistiinopiskeluEnumConverters;
import fi.uta.ristiinopiskelu.persistence.aspect.UncategorizedElasticsearchExceptionAspect;
import fi.uta.ristiinopiskelu.persistence.repository.impl.ExtendedRepositoryImpl;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableElasticsearchRepositories(basePackages = "fi.uta.ristiinopiskelu.persistence.repository", repositoryBaseClass = ExtendedRepositoryImpl.class)
@ComponentScan(basePackages = "fi.uta.ristiinopiskelu.persistence.repository.impl")
public class EsConfig extends ElasticsearchConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(EsConfig.class);

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
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper(javaTimeModule(), jdk8Module())));
        return new ElasticsearchClient(transport);
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

        List<Converter<?, ?>> allConverters = new ArrayList<>();
        List<Converter<?, ?>> currentVersionConverters = RistiinopiskeluEnumConverters.getConverters();
        List<Converter<?, ?>> previousVersionConverters = fi.uta.ristiinopiskelu.datamodel.converter.v8.RistiinopiskeluEnumConverters.getConverters();
        allConverters.addAll(currentVersionConverters);
        allConverters.addAll(previousVersionConverters);

        return new ElasticsearchCustomConversions(allConverters);
    }

    @Override
    public ClientConfiguration clientConfiguration() {
        Assert.hasText(hosts, "ElasticSearch hosts cannot be empty");

        boolean haveCredentials = StringUtils.hasText(username) && StringUtils.hasText(password);

        ClientConfiguration clientConfiguration;
        if(haveCredentials) {
            SSLContext context;
            try {
                context = getSSLContext();
            } catch (Exception e){
                throw new IllegalStateException(e);
            }

            Assert.hasText(caCertificatePath, "ElasticSearch CA certificate path cannot be empty");
            clientConfiguration = ClientConfiguration.builder()
                .connectedTo(StringUtils.commaDelimitedListToStringArray(hosts.trim()))
                .usingSsl(context)
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

    // Jackson modules
    @Bean
    public Module javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    public Module jdk8Module() {
        return new Jdk8Module();
    }

    @Primary
    @Bean("objectMapper")
    public ObjectMapper objectMapper(Module javaTimeModule, Module jdk8Module) {
        SimpleModule sanitizerModule = new SimpleModule();
        sanitizerModule.addDeserializer(String.class, new HtmlSanitizingStringDeserializer());

        SimpleModule offsetDateTimeConverterModule = new SimpleModule();
        offsetDateTimeConverterModule.addSerializer(OffsetDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(OffsetDateTime offsetDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeString(DateUtils.getFormatter().format(offsetDateTime));
            }
        });

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(javaTimeModule)
                .registerModule(jdk8Module)
                .registerModule(sanitizerModule)
                .registerModule(offsetDateTimeConverterModule);

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        objectMapper.setDateFormat(new SimpleDateFormat(DateUtils.getPattern()));
        
        return objectMapper;
    }

    private SSLContext getSSLContext() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        InputStream caCertStream = new FileInputStream(caCertificatePath);

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate caCert = (X509Certificate)certificateFactory.generateCertificate(caCertStream);
        caCertStream.close();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        ks.setCertificateEntry("caCert", caCert);
        trustManagerFactory.init(ks);

        SSLContext context  = SSLContext.getInstance("TLS");
        context.init(null, trustManagerFactory.getTrustManagers(),null);
        return context;
    }
}
