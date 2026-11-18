package ec.edu.udla.sriant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SriAntApplication {
    public static void main(String[] args) {
        SpringApplication.run(SriAntApplication.class, args);
    }
}
