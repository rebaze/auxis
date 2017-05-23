package com.rebaze.auxis.maven;

import com.rebaze.auxis.model.AuxisModel;
import com.rebaze.auxis.model.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EnableNeo4jRepositories(basePackageClasses = AuxisModel.class)
public class AuxisMavenIndexerApplication {

    private final static Logger log = LoggerFactory.getLogger(AuxisMavenIndexerApplication.class);

    @Autowired
    MavenIndexer indexer;

    public static void main(String[] args) {
        SpringApplication.run(AuxisMavenIndexerApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner() {
        String name = "central";
        String url = "https://nexus.psp.cardtech.de/nexus/repository/maven-thirdparty";
        //String url = "http://repo1.maven.org/maven2";


        return args -> {
            indexer.perform(name,url);
        };
    }
}
