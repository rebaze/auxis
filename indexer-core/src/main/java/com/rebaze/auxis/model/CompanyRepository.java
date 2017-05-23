package com.rebaze.auxis.model;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface CompanyRepository extends GraphRepository<Company> {

    Company findByName(String name);
}
