package fi.uta.ristiinopiskelu.examples.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Example controller for proxying requests to RIPA handler's REST API
 *
 * See RestTemplate configuration: ExampleApplicationConfig#restTemplate()
 */
@RequestMapping("/api/studies")
@RestController
public class ExampleStudiesController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${general.api-url}")
    private String apiUrl;

    @GetMapping("/courseunits")
    public String getStudies(@RequestParam(value = "courseUnitId", required = false) String courseUnitId) {
        StringBuilder sb = new StringBuilder(apiUrl + "/api/v8/studies/courseunits");

        if(StringUtils.hasText(courseUnitId)) {
            sb.append("?courseUnitId=" + courseUnitId);
        }

        return restTemplate.getForObject(sb.toString(), String.class);
    }

    @GetMapping("/networks")
    public String getNetworks() {
        return restTemplate.getForObject(apiUrl + "/api/v8/networks", String.class);
    }
}
