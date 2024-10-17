package org.hibernate.build.maven.embedder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.cli.MavenCli;

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * @author Steve Ebersole
 */
public abstract class MavenEmbedderService implements BuildService<MavenEmbedderService.Config> {
	interface Config extends BuildServiceParameters {
		DirectoryProperty getWorkingDirectory();
		DirectoryProperty getMavenLocalDirectory();
	}

	private final MavenCli embedder;

	public MavenEmbedderService() {
		System.setProperty( "maven.multiModuleProjectDirectory", getParameters().getWorkingDirectory().toString() );
		embedder = new MavenCli();
	}

	public void execute(String... tasksAndArgs) {
		final List<String> cml = new ArrayList<>();
		Collections.addAll( cml, tasksAndArgs );

		final Directory mavenLocalDirectory = getParameters().getMavenLocalDirectory().get();
		cml.add( "-Dmaven.repo.local=" + mavenLocalDirectory.getAsFile().getAbsolutePath() );

		final Directory workingDirectory = getParameters().getWorkingDirectory().get();
		final String workingDirectoryPath = workingDirectory.getAsFile().getAbsolutePath();

		final int resultCode = embedder.doMain( cml.toArray(new String[0]), workingDirectoryPath, System.out, System.err );
	}
}
