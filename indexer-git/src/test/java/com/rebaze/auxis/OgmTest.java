package com.rebaze.auxis;

import com.rebaze.auxis.model.Company;
import com.rebaze.auxis.model.Project;
import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

public class OgmTest {

    private final static SessionFactory sessionFactory = new SessionFactory("com.rebaze.auxis.model");

    public Session getNeo4jSession() {
        return sessionFactory.openSession();
    }

    @Test
    public void testOgmDirect() {
        Session s = getNeo4jSession();
        Company company = new Company("Foo Corporation");
        s.save(company);
        Project project = new Project("Stark Project","","");
        s.save(project);
    }
}
