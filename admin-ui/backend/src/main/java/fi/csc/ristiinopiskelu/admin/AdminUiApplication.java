package fi.csc.ristiinopiskelu.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={
        "fi.csc.ristiinopiskelu.admin.config",
        "fi.csc.ristiinopiskelu.admin.services",
        "fi.csc.ristiinopiskelu.admin.controller"
})
public class AdminUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminUiApplication.class, args);
    }
}
