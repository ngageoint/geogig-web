package org.geogig.web.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

    private static Properties properties;

    public static VersionInfo get() {
        if (null == properties) {
            properties = loadProperties();
        }

        // try to get the implementation version from this class
        String projectVersion = VersionInfo.class.getPackage().getImplementationVersion();
        if (projectVersion == null) {
            // no Implementation Version available
            // should only occur if running from class files and not a JAR
            projectVersion = "UNDETERMINED";
        }
        String branch = properties.get("git.branch").toString();
        String commitId = properties.get("git.commit.id").toString();
        // String commitIdAbbrev = properties.get("git.commit.id.abbrev").toString();
        // String buildUserName = properties.get("git.build.user.name").toString();
        // String buildUserEmail = properties.get("git.build.user.email").toString();
        String buildTime = properties.get("git.build.time").toString();
        // String commitUserName = properties.get("git.commit.user.name").toString();
        // String commitUserEmail = properties.get("git.commit.user.email").toString();
        // String commitMessageShort = properties.get("git.commit.message.short").toString();
        String commitMessageFull = properties.get("git.commit.message.full").toString();
        String commitTime = properties.get("git.commit.time").toString();

        VersionInfo version = new VersionInfo();
        version.version(projectVersion)//
                .branch(branch)//
                .commitId(commitId)//
                .buildTime(buildTime)//
                .commitTime(commitTime)//
                .commitMessage(commitMessageFull);
        return version;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream resource = Version.class.getResourceAsStream("git.properties")) {
            if (null == resource) {
                throw new IllegalStateException(
                        Version.class.getPackage().getName() + "/git.properties not found");
            }
            properties.load(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
