package org.hibernate.build.maven.embedder;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

/**
 * Gradle DSL extension for configuring the {@linkplain MavenEmbedderPlugin maven-embedder plugin}
 *
 * @author Steve Ebersole
 */
public class MavenEmbedderConfig {
	private DirectoryProperty workingDirectory;
	private DirectoryProperty localRepositoryDirectory;

	@Inject
	public MavenEmbedderConfig(Project project) {
		final DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();

		workingDirectory = project.getObjects().directoryProperty();
		workingDirectory.convention( buildDirectory.dir( "maven-embedder/working" ) );

		localRepositoryDirectory = project.getObjects().directoryProperty();
		localRepositoryDirectory.convention( buildDirectory.dir( "maven-embedder/maven-local" ) );
	}

	public DirectoryProperty getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(DirectoryProperty workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public DirectoryProperty getLocalRepositoryDirectory() {
		return localRepositoryDirectory;
	}

	public void setLocalRepositoryDirectory(DirectoryProperty localRepositoryDirectory) {
		this.localRepositoryDirectory = localRepositoryDirectory;
	}
}
