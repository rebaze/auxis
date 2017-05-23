package com.rebaze.auxis.maven;

import lombok.Data;

/**
 * Created by tonit on 11.05.17.
 */
@Data
public class ArtifactCandidate {
    private String groupId;
    private String artifactId;
    private String sha1;
    private String version;
    private String scmConnection;
}
