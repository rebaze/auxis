package com.rebaze.auxis.model

import org.neo4j.ogm.annotation.GraphId
import org.neo4j.ogm.annotation.NodeEntity
import org.springframework.data.neo4j.repository.Neo4jRepository


interface ProjectRepository : Neo4jRepository<Project,Long> {

    fun findByName(name: String): Project
}

interface CompanyRepository : Neo4jRepository<Company,Long> {

    fun findByName(name: String): Company
}

interface PersonRepository : Neo4jRepository<Person,Long> {

    fun findByName(name: String): Person
}

@NodeEntity open class Company (var name : String?) {

    @GraphId
    //private var id: Long = 0
    private var id : java.lang.Long = java.lang.Long(0)


   // private constructor() : this(null)
}
