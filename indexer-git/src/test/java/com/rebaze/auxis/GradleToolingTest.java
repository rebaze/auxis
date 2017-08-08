package com.rebaze.auxis;

import java.io.File;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.junit.Test;

public class GradleToolingTest {
    @Test
    public void testGradle() {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(new File("/Users/tonit/devel/spicter/com.spicter.core"))
                .connect();

        try {
            connection.newBuild().forTasks("tasks").addProgressListener((ProgressListener)
                    event -> System.out.println("-->" + event.getDescription())).run();

        } finally {
            connection.close();
        }
    }
}
