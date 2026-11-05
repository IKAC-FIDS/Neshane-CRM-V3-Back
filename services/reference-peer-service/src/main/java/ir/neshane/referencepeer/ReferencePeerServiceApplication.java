package ir.neshane.referencepeer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReferencePeerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferencePeerServiceApplication.class, args);
    }
}
