package com.rebaze.auxis.model;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class Company {

    @GraphId
    private Long id;

    @Getter
    @Setter
    private String name;

    public Company(String company) {
        this.name = company;
    }

    private Company() {

    }
}
