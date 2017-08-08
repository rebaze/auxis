package com.rebaze.auxis.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Person {

    @GraphId private Long id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String email;

    private Person() {
        // Empty constructor required as of Neo4j API 2.0.5
    };

    public Person(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Relationship(type = "PARTICIPATES", direction = Relationship.UNDIRECTED)
    private Set<Project> projects;

    @Relationship(type = "WORKS", direction = Relationship.UNDIRECTED)
    private Company company;

    public void worksFor(Company company) {
        this.company = company;
    }

    public void worksIn(Project p) {
        if (projects == null) {
            projects = new HashSet<>();
        }
        projects.add(p);
    }

    public String toString() {

        return this.name + "'s projects => "
                + Optional.ofNullable(this.projects).orElse(
                Collections.emptySet()).stream().map(
                Project::getName).collect(Collectors.toList());
    }
}
