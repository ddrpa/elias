package cc.ddrpa.dorian.elias.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorCompileClasspathTest {

    @TempDir
    Path temp;

    @Test
    void findReactorProjectMatchesGroupArtifactWithoutClassifier() {
        MavenProject sibling = project("io.example", "lib", temp.resolve("lib/target/classes"));
        Artifact artifact = artifact("io.example", "lib", "1.0-SNAPSHOT", "compile", null);
        assertSame(sibling, ReactorCompileClasspath.findReactorProject(artifact, List.of(sibling)));
    }

    @Test
    void findReactorProjectIgnoresClassifiedArtifacts() {
        MavenProject sibling = project("io.example", "lib", temp.resolve("lib/target/classes"));
        Artifact tests = artifact("io.example", "lib", "1.0-SNAPSHOT", "compile", "tests");
        assertEquals(null, ReactorCompileClasspath.findReactorProject(tests, List.of(sibling)));
    }

    @Test
    void buildPrefersReactorClassesOverExternalResolver() throws Exception {
        Path currentClasses = temp.resolve("app/target/classes");
        Path siblingClasses = temp.resolve("lib/target/classes");
        Files.createDirectories(currentClasses);
        Files.createDirectories(siblingClasses);

        MavenProject current = project("io.example", "app", currentClasses);
        MavenProject sibling = project("io.example", "lib", siblingClasses);
        Artifact lib = artifact("io.example", "lib", "1.0-SNAPSHOT", "compile", null);
        Artifact guava = artifact("com.google.guava", "guava", "32.0.0-jre", "compile", null);
        current.setArtifacts(Set.of(lib, guava));

        Path guavaJar = temp.resolve("guava.jar");
        Files.writeString(guavaJar, "jar");
        AtomicInteger externalCalls = new AtomicInteger();

        List<File> classpath = ReactorCompileClasspath.build(
                current,
                List.of(current, sibling),
                artifact -> {
                    externalCalls.incrementAndGet();
                    assertEquals("guava", artifact.getArtifactId());
                    return guavaJar.toFile();
                });

        assertEquals(1, externalCalls.get(), "reactor module must not be resolved from repo");
        assertEquals(currentClasses.toFile().getAbsoluteFile(), classpath.get(0));
        assertEquals(3, classpath.size());
        assertTrue(classpath.contains(siblingClasses.toFile().getAbsoluteFile()));
        assertTrue(classpath.contains(guavaJar.toFile().getAbsoluteFile()));
    }

    @Test
    void buildFailsWhenCurrentClassesMissing() {
        MavenProject current = project("io.example", "app", temp.resolve("missing/classes"));
        current.setArtifacts(Set.of());
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> ReactorCompileClasspath.build(current, List.of(current), a -> null));
        assertTrue(ex.getMessage().contains("Compile this module first"));
    }

    @Test
    void buildFailsWhenReactorClassesMissing() throws Exception {
        Path currentClasses = temp.resolve("app/target/classes");
        Files.createDirectories(currentClasses);
        MavenProject current = project("io.example", "app", currentClasses);
        MavenProject sibling = project("io.example", "lib", temp.resolve("lib/target/classes"));
        current.setArtifacts(Set.of(
                artifact("io.example", "lib", "1.0-SNAPSHOT", "compile", null)));

        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> ReactorCompileClasspath.build(current, List.of(current, sibling), a -> null));
        assertTrue(ex.getMessage().contains("io.example:lib"));
        assertTrue(ex.getMessage().contains("Compile that module first"));
    }

    @Test
    void runtimeScopeExcludedFromCompileClasspath() {
        Artifact runtime = artifact("io.example", "x", "1", Artifact.SCOPE_RUNTIME, null);
        assertEquals(false, ReactorCompileClasspath.isCompileClasspathArtifact(runtime));
    }

    private static MavenProject project(String groupId, String artifactId, Path outputDirectory) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion("1.0-SNAPSHOT");
        Build build = new Build();
        build.setOutputDirectory(outputDirectory.toAbsolutePath().toString());
        project.setBuild(build);
        project.setArtifacts(new HashSet<>());
        return project;
    }

    private static Artifact artifact(
            String groupId, String artifactId, String version, String scope, String classifier) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler("jar");
        handler.setAddedToClasspath(true);
        DefaultArtifact artifact = new DefaultArtifact(
                groupId, artifactId, version, scope, "jar", classifier, handler);
        return artifact;
    }
}
