package fi.uta.ristiinopiskelu.handler.config;

import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class SpringDocConfig {

    @Bean
    public GroupedOpenApi currentVersionApiDoc(MessageSchemaService messageSchemaService) {
        return buildGroupedOpenApi(messageSchemaService.getCurrentSchemaVersion());
    }

    @Bean
    public GroupedOpenApi previousVersionApiDoc(MessageSchemaService messageSchemaService) {
        return buildGroupedOpenApi(messageSchemaService.getPreviousSchemaVersion());
    }

    private GroupedOpenApi buildGroupedOpenApi(int version) {
        String versionString = String.format("v%s", version);

        return GroupedOpenApi.builder()
            .group(versionString)
            .displayName(versionString)
            .packagesToScan(String.format("fi.uta.ristiinopiskelu.handler.controller.%s", versionString))
            .pathsToExclude("/error.*")
            .addOpenApiCustomizer(openApi -> {
                Server server = new Server();
                server.setUrl("https://risti-lb.csc.fi/handler/");
                openApi.setServers(Collections.singletonList(server));
                openApi.setInfo(new Info()
                    .title("Ristiinopiskelupalvelun rajapintadokumentaatio")
                    .version(versionString));
            })
            .build();         
    }
}
