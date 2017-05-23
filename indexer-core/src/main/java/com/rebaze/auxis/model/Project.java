package com.rebaze.auxis.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Project {

    @GraphId
    private Long id;

    @Getter
    private String name;

    @Getter
    private String path;

    @Getter
    private String origin;

    public Project(String name, String path, String origin) {
        this.name = name;
        this.path = path;
        this.origin = origin;
    }

    private Project() {
        // Empty constructor required as of Neo4j API 2.0.5
    };

    @Relationship(type = "ACTS_IN", direction = Relationship.OUTGOING)
    private Set<Person> actors = new HashSet<>();
}
