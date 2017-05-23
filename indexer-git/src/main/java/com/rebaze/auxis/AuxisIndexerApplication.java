package com.rebaze.auxis;

import com.rebaze.auxis.api.Indexer;
import com.rebaze.auxis.model.PersonRepository;
import java.io.File;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EnableNeo4jRepositories
public class AuxisIndexerApplication {

    private final static Logger log = LoggerFactory.getLogger(AuxisIndexerApplication.class);

    @Autowired
    Indexer indexer;

    public static void main(String[] args) {
        SpringApplication.run(AuxisIndexerApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(PersonRepository personRepository) {
        return args -> {
            indexer.index(new File("/Users/tonit/devel/spicter/evasion").toURI().toURL());
        };
    }

    //@Bean
    public CommandLineRunner commandLineRunner2(ApplicationContext ctx) {
        return args -> {

            System.out.println("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }

        };
    }
}
