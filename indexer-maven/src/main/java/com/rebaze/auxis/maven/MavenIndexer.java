package com.rebaze.auxis.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.rebaze.auxis.model.CompanyRepository;
import com.rebaze.auxis.model.PersonRepository;
import com.rebaze.auxis.model.ProjectRepository;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.Field;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MavenIndexer {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CompanyRepository companyRepository;

    // ==

    private final PlexusContainer plexusContainer;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;


    public MavenIndexer()
            throws PlexusContainerException, ComponentLookupException {
        // here we create Plexus container, the Maven default IoC container
        // Plexus falls outside of MI scope, just accept the fact that
        // MI is a Plexus component ;)
        // If needed more info, ask on Maven Users list or Plexus Users list
        // google is your friend!
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
        this.plexusContainer = new DefaultPlexusContainer(config);

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup(Indexer.class);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup(Wagon.class, "http");

    }

    public void perform(String name, String url)
            throws Exception {
        IndexingContext centralContext = createIndexingContext(name, url);
        //updateIndex(centralContext);
        Set<ArtifactCandidate> result = search(centralContext);
        Set<String> scms = new HashSet<>();
        for(ArtifactCandidate c : result) {
            scms.add(c.getScmConnection());
        }
        scms.forEach(System.out::println);
        System.out.println("GA: " + result.size() + " in " + scms.size() + " repositories");
        indexer.closeIndexingContext(centralContext, false);
    }

    private IndexingContext createIndexingContext(String name, String url) throws ComponentLookupException, IOException {
        File centralLocalCache = new File("target/" + name + "-cache");
        File centralIndexDir = new File("target/" + name + "-index");

        // Creators we want to use (search for fields it defines)
        List<IndexCreator> indexers = new ArrayList<IndexCreator>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "jarContent"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "maven-plugin"));


        // Create context for central repository index

        return indexer.createIndexingContext(name + "-context", name, centralLocalCache, centralIndexDir,
                url, null, true, true, indexers);
    }

    private Set<ArtifactCandidate> search(IndexingContext centralContext) throws Exception {
        MavenTransportSystem maven = new MavenTransportSystem();
        maven.activate(plexusContainer);

        Query query = indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression("org.ops4j.*"));
        //Query query = new MatchAllDocsQuery();
        FlatSearchResponse r = indexer.searchFlat(new FlatSearchRequest(query, centralContext));
        System.out.println("Hits: " + r.getReturnedHitsCount());

        Set<GroupedArtifact> found = new HashSet<>();
         for (ArtifactInfo ai : r.getResults()) {
            found.add(createGA(ai));
        }
        System.out.println("Dimensions: GA: " + found.size());

        Set<ArtifactCandidate> candudates = new HashSet<>();
        // groups:
        for (GroupedArtifact art : found) {
            ArtifactCandidate candidate = createBestPomUrl(centralContext,art);
            maven.getSourceRepository(candidate);
            candudates.add(candidate);
        }
        return candudates;
    }

    private ArtifactCandidate createBestPomUrl(IndexingContext centralContext, GroupedArtifact ga) throws Exception {
        final GenericVersionScheme versionScheme = new GenericVersionScheme();

        final Query groupIdQ =
                indexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( ga.getGroupId() ) );
        final Query artifactIdQ =
                indexer.constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression( ga.getArtifactId() ) );
        final BooleanQuery query = new BooleanQuery();
        query.add( groupIdQ, Occur.MUST );
        query.add( artifactIdQ, Occur.MUST );

        // we want "jar" artifacts only
        query.add( indexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression( "jar" ) ), Occur.SHOULD );
        query.add( indexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression( "bundle" ) ), Occur.SHOULD );

        // we want main artifacts only (no classifier)
        // Note: this below is unfinished API, needs fixing
        query.add( indexer.constructQuery( MAVEN.CLASSIFIER, new SourcedSearchExpression( Field.NOT_PRESENT ) ),
                Occur.MUST_NOT );

        FlatSearchResponse r = indexer.searchFlat(new FlatSearchRequest(query, centralContext));
        Set<String> classif = new HashSet<>();

        ArtifactCandidate candidate = new ArtifactCandidate();
        Version highestVersion = versionScheme.parseVersion( "0" );
        candidate.setGroupId(ga.getGroupId());
        candidate.setArtifactId(ga.getArtifactId());

        for (ArtifactInfo ai : r.getResults()) {
            final Version aiV = versionScheme.parseVersion( ai.version );
            // Use ">=" if you are INCLUSIVE
            if (aiV.compareTo( highestVersion ) > 0) {
                highestVersion = aiV;
                candidate.setSha1(ai.sha1);
                candidate.setVersion(ai.version);
            }
        }
        if (candidate.getSha1() == null) {
            throw new IllegalStateException("GA " + ga + " has no valid artifact.");
        }
        return candidate;
    }


    private GroupedArtifact createGA(ArtifactInfo ai) {
        GroupedArtifact ga = new GroupedArtifact();
        ga.setGroupId(ai.groupId);
        ga.setArtifactId(ai.artifactId);
        return ga;
    }

    private void updateIndex(IndexingContext centralContext) throws IOException {
        // Preferred frequency is once a week.

        System.out.println("Updating Index...");
        System.out.println("This might take a while on first run, so please be patient!");
        // Create ResourceFetcher implementation to be used with IndexUpdateRequest
        // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
        TransferListener listener = getTransferListener();
        ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);

        Date centralContextCurrentTimestamp = centralContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
        IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        System.out.println("ref: " + centralContextCurrentTimestamp + " update: " + updateResult.getTimestamp());

        if (updateResult.isFullUpdate()) {
            System.out.println("Full update happened!");
        } else if (updateResult.getTimestamp() == null || updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
            System.out.println("No update needed, index is up to date!");
        } else {
            System.out.println(
                    "Incremental update happened, change covered " + centralContextCurrentTimestamp + " - "
                            + updateResult.getTimestamp() + " period.");
        }

        System.out.println();

    }

    private TransferListener getTransferListener() {
        return new AbstractTransferListener() {
            public void transferStarted(TransferEvent transferEvent) {
                System.out.print("  Downloading " + transferEvent.getResource().getName());
            }

            public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
            }

            public void transferCompleted(TransferEvent transferEvent) {
                System.out.println(" - Done");
            }
        };
    }
}
