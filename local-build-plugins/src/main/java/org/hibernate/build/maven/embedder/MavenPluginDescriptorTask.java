package org.hibernate.build.maven.embedder;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Steve Ebersole
 */
public abstract class MavenPluginDescriptorTask extends DefaultTask {
	// This property provides access to the service instance
	@ServiceReference
	abstract Property<MavenEmbedderService> getMavenEmbedderService();

	@OutputDirectory
	abstract DirectoryProperty getDescriptorDirectory();

	public MavenPluginDescriptorTask() {
		// todo : what else is the descriptor dependent upon?
		getInputs().property( "project-version", getProject().getVersion() );
	}

	@TaskAction
	public void generateDescriptor() {
		getMavenEmbedderService().get().getEmbedderDelegate().execute( "plugin:descriptor" );
//		getMavenEmbedderService().get().execute( "plugin:addPluginArtifactMetadata" );
	}
}
