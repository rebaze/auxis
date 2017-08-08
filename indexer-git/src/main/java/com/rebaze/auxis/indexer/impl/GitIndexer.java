package com.rebaze.auxis.indexer.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rebaze.auxis.AuxisIndexerApplication;
import com.rebaze.auxis.api.Indexer;
import com.rebaze.auxis.model.Company;
import com.rebaze.auxis.model.CompanyRepository;
import com.rebaze.auxis.model.Person;
import com.rebaze.auxis.model.PersonRepository;
import com.rebaze.auxis.model.Project;
import com.rebaze.auxis.model.ProjectRepository;
import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GitIndexer implements Indexer {

    private final static Logger log = LoggerFactory.getLogger(AuxisIndexerApplication.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private ConcurrentHashMap<String, Project> projects = new ConcurrentHashMap<>();

    @Override
    public void index(URL path) throws Exception {
        personRepository.deleteAll();
        projectRepository.deleteAll();
        companyRepository.deleteAll();
        companyRepository.save(new Company("Evil Corp"));
/**
        ObjectMapper jsonMapper = new ObjectMapper();
        GitSeedConfig configs = jsonMapper.readValue(new File("seeds/gitrepos.json"), GitSeedConfig.class);
        for (GitSourceConfig source : configs.getSources()) {
            if (source.getEnabled()) {
                Project project = loadProject(source.getName(), source.getUrl());
                projectRepository.save(project);

                //scanGitHistory(project);
            }
        }
 **/
    }

    private Project loadProject(String name, String path) {
        return projects.computeIfAbsent(name, n -> new Project(n, path,""));
    }

    private void scanGitHistory(Project project)
            throws Exception {
        Git repos = Git.open(new File(project.getPath()));
        Ref head = repos.getRepository().findRef("HEAD");

        RevWalk rw = new RevWalk(repos.getRepository());
        RevCommit commit = rw.parseCommit(head.getObjectId());
        RevTree tree = commit.getTree();
        System.out.println("Having commit: " + commit);

        TreeWalk treeWalk = new TreeWalk(repos.getRepository());
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        Set<String> authors = new HashSet<>();
        Set<String> paths = new HashSet<>();
        while (treeWalk.next()) {
            paths.add(treeWalk.getPathString());
        }

        paths.parallelStream().forEach(p -> {

            BlameCommand blameCommand = new BlameCommand(repos.getRepository());
            blameCommand.setFilePath(p);
            BlameResult res = null;
            try {
                res = blameCommand.call();
                RawText raw = res.getResultContents();
                int lines = raw.size();
                for (int i = 0; i < lines; i++) {

                    PersonIdent person = res.getSourceCommitter(i);
                    if (person != null) {
                        authors.add(person.getEmailAddress());
                    }
                }
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        });

        for (String author : authors) {
            Person p = fetchPerson(author);
            p.worksIn(project);
            p.worksFor(fetchCompany(author));
            personRepository.save(p);
        }
    }

    private Company fetchCompany(String author) {
        String company = author.substring(author.indexOf("@") + 1).toLowerCase();
        if (company.contains(".")) {
            company = company.substring(0,company.lastIndexOf("."));
        }
        Company c = companyRepository.findByName(company);
        if (c == null) {
            c = new Company(company);
        }
        return c;
    }

    private Person fetchPerson(String author) {
        Person p = personRepository.findByName(author.toLowerCase());
        if (p == null) {
            p = new Person(author.toLowerCase(), author.toLowerCase());
        }
        return p;
    }
}
