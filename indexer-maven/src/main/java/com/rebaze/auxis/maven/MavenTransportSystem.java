package com.rebaze.auxis.maven;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;


public class MavenTransportSystem {

    private RepositorySystem m_repoSystem;
    private LocalRepository localRepository;
    private PlexusContainer m_plexus;
    private String prefix;

    public void activate(PlexusContainer plexusContainer) throws ComponentLookupException {
        m_repoSystem = newRepositorySystem(plexusContainer);
        m_plexus = plexusContainer;
    }

    private RepositorySystemSession createSession(LocalRepository repo) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        if (repo != null) {
            session.setLocalRepositoryManager(m_repoSystem.newLocalRepositoryManager(session, repo));
        } else {
            session.setLocalRepositoryManager(m_repoSystem.newLocalRepositoryManager(session, getLocalRepository()));
        }
        session.setOffline(false);

        return session;
    }

    private RepositorySystem newRepositorySystem(PlexusContainer plexusContainer) throws ComponentLookupException {
        return plexusContainer.lookup(RepositorySystem.class);
        //DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        /**
         locator.addService( TransporterFactory.class, WagonTransporterFactory.class );
         locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);

         locator.setService( org.eclipse.aether.spi.log.LoggerFactory.class,
         Slf4jLoggerFactory.class );

         return locator.getService( RepositorySystem.class );
         **/
    }

    public void getSourceRepository(ArtifactCandidate candidate) throws ArtifactResolutionException, ArtifactDescriptorException, ComponentLookupException, URISyntaxException {
        RepositorySystemSession session = createSession(getLocalRepository());
        Artifact artifact = new DefaultArtifact(candidate.getGroupId() + ":" + candidate.getArtifactId() + ":" + candidate.getVersion());
        //Artifact artifact = new DefaultArtifact( "org.eclipse.aether:aether-util:1.0.0.v20140518" );



        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(newRepositories(m_repoSystem, session));

        /**
         ArtifactResult artifactResult = m_repoSystem.resolveArtifact( session, artifactRequest );
         artifact = artifactResult.getArtifact();

         System.out.println( artifact + " resolved to  " + artifact.getFile() );
         **/
        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact(artifact);
        descriptorRequest.setRepositories(newRepositories(m_repoSystem, session));
        ArtifactDescriptorResult result = new ArtifactDescriptorResult(descriptorRequest);

        Model model = loadPom(session,descriptorRequest,result);

        if (model == null || model.getScm() == null) {
           // System.out.println(" - " + candidate);
        } else {
            //System.out.println(" + " + candidate + " is " + model.getScm().getConnection() + " on Tag " + model.getScm().getTag());
            candidate.setScmConnection( parseRepository(model.getScm().getConnection() ) );
        }
    }

    private String parseRepository(String url) throws URISyntaxException {
        prefix = "scm:git";
        if (url.startsWith(prefix)) {
            url = url.substring(prefix.length()+1);
            //URI uri = new URI(url);
           // return uri.toASCIIString();

            if (url.endsWith(".git")) {
                return url;
            }else if (url.contains(".git")) {
                return url.substring(0,url.indexOf(".git") + 4);
            }else if (url.startsWith("git@github.com")) {
                // add .git to normalize the style:
                return url + ".git";
            }

        }
        return null;
    }

    private LocalRepository getLocalRepository() {
        if (localRepository == null) {
            File local = new File(System.getProperty("user.home"), ".m2/repository");
            localRepository = new LocalRepository(local, "simple");
        }
        return localRepository;
    }


    public static void newRepositorySystemOld() {
        // return org.eclipse.aether.examples.manual.ManualRepositorySystemFactory.newRepositorySystem();
        // return org.eclipse.aether.examples.guice.GuiceRepositorySystemFactory.newRepositorySystem();
        // return org.eclipse.aether.examples.sisu.SisuRepositorySystemFactory.newRepositorySystem();
        // return org.eclipse.aether.examples.plexus.PlexusRepositorySystemFactory.newRepositorySystem();
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        //session.setTransferListener( new ConsoleTransferListener() );
        //session.setRepositoryListener( new ConsoleRepositoryListener() );

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    public static List<RemoteRepository> newRepositories(RepositorySystem system, RepositorySystemSession session) {
        return new ArrayList<RemoteRepository>(Arrays.asList(newCentralRepository()));
    }

    private static RemoteRepository newCentralRepository() {
        return new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
    }

    private Model loadPom(RepositorySystemSession session, ArtifactDescriptorRequest request,
                          ArtifactDescriptorResult result)
            throws ArtifactDescriptorException, ComponentLookupException {

        VersionResolver versionResolver = m_plexus.lookup(VersionResolver.class);

        VersionRangeResolver versionRangeResolver = m_plexus.lookup(VersionRangeResolver.class);

        ArtifactResolver artifactResolver = m_plexus.lookup(ArtifactResolver.class);

        RemoteRepositoryManager remoteRepositoryManager = m_plexus.lookup(RemoteRepositoryManager.class);

        //RepositoryEventDispatcher repositoryEventDispatcher  =m_plexus.lookup(RepositoryEventDispatcher.class);

        ModelBuilder modelBuilder = m_plexus.lookup(ModelBuilder.class);

        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        Set<String> visited = new LinkedHashSet<>();
        for (Artifact a = request.getArtifact(); ; ) {
            Artifact pomArtifact = ArtifactDescriptorUtils.toPomArtifact(a);
            try {
                VersionRequest versionRequest =
                        new VersionRequest(a, request.getRepositories(), request.getRequestContext());
                versionRequest.setTrace(trace);

                VersionResult versionResult = versionResolver.resolveVersion(session, versionRequest);

                a = a.setVersion(versionResult.getVersion());

                versionRequest =
                        new VersionRequest(pomArtifact, request.getRepositories(), request.getRequestContext());
                versionRequest.setTrace(trace);
                versionResult = versionResolver.resolveVersion(session, versionRequest);

                pomArtifact = pomArtifact.setVersion(versionResult.getVersion());
            } catch (VersionResolutionException e) {
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            if (!visited.add(a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getBaseVersion())) {
                RepositoryException exception =
                        new RepositoryException("Artifact relocations form a cycle: " + visited);

                return null;

            }

            ArtifactResult resolveResult;
            try {
                ArtifactRequest resolveRequest =
                        new ArtifactRequest(pomArtifact, request.getRepositories(), request.getRequestContext());
                resolveRequest.setTrace(trace);
                resolveResult = artifactResolver.resolveArtifact(session, resolveRequest);
                pomArtifact = resolveResult.getArtifact();
                result.setRepository(resolveResult.getRepository());
            } catch (ArtifactResolutionException e) {
                if (e.getCause() instanceof ArtifactNotFoundException) {

                    return null;

                }
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            Model model;

            try {
                ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
                modelRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                modelRequest.setProcessPlugins(false);
                modelRequest.setTwoPhaseBuilding(false);

               // modelRequest.setModelCache(DefaultModelCache.newInstance(session));
                modelRequest.setModelResolver(new DefaultModelResolver(session, trace.newChild(modelRequest),
                        request.getRequestContext(), artifactResolver,
                        versionRangeResolver, remoteRepositoryManager,
                        request.getRepositories()));
                if (resolveResult.getRepository() instanceof WorkspaceRepository) {
                    modelRequest.setPomFile(pomArtifact.getFile());
                } else {
                    modelRequest.setModelSource(new FileModelSource(pomArtifact.getFile()));
                }

                model = modelBuilder.build(modelRequest).getEffectiveModel();
            } catch (ModelBuildingException e) {
                for (ModelProblem problem : e.getProblems()) {
                    if (problem.getException() instanceof UnresolvableModelException) {
                        result.addException(problem.getException());
                        throw new ArtifactDescriptorException(result);
                    }
                }

                return null;

            }



            return model;

        }
    }
}
