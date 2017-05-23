package com.rebaze.auxis.model;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface ProjectRepository extends GraphRepository<Project> {

    Project findByName(String name);
}
