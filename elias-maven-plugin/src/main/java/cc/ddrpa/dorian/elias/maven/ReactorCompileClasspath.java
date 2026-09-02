package cc.ddrpa.dorian.elias.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds a compile classpath that prefers reactor module {@code target/classes} over repository
 * artifacts, so sibling SNAPSHOTs need not be installed.
 */
final class ReactorCompileClasspath {

    @FunctionalInterface
    interface ExternalArtifactResolver {
        File resolve(Artifact artifact) throws Exception;
    }

    private ReactorCompileClasspath() {
    }

    /**
     * Ordered classpath entries: current module output directory, then compile-scoped deps
     * (reactor {@code target/classes} or resolved external files).
     */
    static List<File> build(
            MavenProject project,
            List<MavenProject> reactorProjects,
            ExternalArtifactResolver externalResolver) throws MojoExecutionException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(externalResolver, "externalResolver");
        List<MavenProject> reactor = reactorProjects == null ? List.of() : reactorProjects;

        Set<File> entries = new LinkedHashSet<>();
        File output = new File(project.getBuild().getOutputDirectory());
        if (!output.isDirectory()) {
            throw new MojoExecutionException(
                    "Project classes not found at " + output.getAbsolutePath()
                            + ". Compile this module first (e.g. mvn compile), then re-run "
                            + "elias:changelog-export.");
        }
        entries.add(output.getAbsoluteFile());

        for (Artifact artifact : project.getArtifacts()) {
            if (!isCompileClasspathArtifact(artifact)) {
                continue;
            }
            MavenProject reactorProject = findReactorProject(artifact, reactor);
            if (reactorProject != null) {
                File reactorClasses = new File(reactorProject.getBuild().getOutputDirectory());
                if (!reactorClasses.isDirectory()) {
                    throw new MojoExecutionException(
                            "Reactor module " + reactorProject.getGroupId() + ":"
                                    + reactorProject.getArtifactId()
                                    + " has no classes at " + reactorClasses.getAbsolutePath()
                                    + ". Compile that module first, then re-run "
                                    + "elias:changelog-export.");
                }
                entries.add(reactorClasses.getAbsoluteFile());
                continue;
            }
            try {
                File resolved = externalResolver.resolve(artifact);
                if (resolved == null || !resolved.exists()) {
                    throw new MojoExecutionException(
                            "Could not resolve dependency " + artifact.getId());
                }
                entries.add(resolved.getAbsoluteFile());
            } catch (MojoExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "Could not resolve dependency " + artifact.getId(), e);
            }
        }
        return new ArrayList<>(entries);
    }

    static MavenProject findReactorProject(Artifact artifact, List<MavenProject> reactorProjects) {
        if (artifact == null || reactorProjects == null || reactorProjects.isEmpty()) {
            return null;
        }
        String classifier = artifact.getClassifier();
        for (MavenProject candidate : reactorProjects) {
            if (!artifact.getGroupId().equals(candidate.getGroupId())
                    || !artifact.getArtifactId().equals(candidate.getArtifactId())) {
                continue;
            }
            // Main project artifact has no classifier; skip classified attachments (e.g. tests).
            if (classifier != null && !classifier.isBlank()) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    static boolean isCompileClasspathArtifact(Artifact artifact) {
        if (artifact == null || artifact.getArtifactHandler() == null
                || !artifact.getArtifactHandler().isAddedToClasspath()) {
            return false;
        }
        String scope = artifact.getScope();
        return scope == null
                || Artifact.SCOPE_COMPILE.equals(scope)
                || Artifact.SCOPE_PROVIDED.equals(scope)
                || Artifact.SCOPE_SYSTEM.equals(scope);
    }
}
