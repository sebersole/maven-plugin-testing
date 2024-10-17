package org.hibernate.build.maven.embedder;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * @author Steve Ebersole
 */
public class MavenEmbedderPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		final BuildServiceRegistry sharedServices = project.getGradle().getSharedServices();
		final SourceSetContainer sourceSets = project.getExtensions().getByType( SourceSetContainer.class );
		final SourceSet mainSourceSet = sourceSets.getByName( "main" );

		// be sure to mirror the output dirs
		project.getLayout().getBuildDirectory().set( project.getLayout().getProjectDirectory().dir( "target" ) );

		final MavenEmbedderConfig dsl = project.getExtensions().create(
				"mavenEmbedder",
				MavenEmbedderConfig.class
		);

		final Provider<MavenEmbedderService> embedderServiceProvider = sharedServices.registerIfAbsent(
				"maven-embedder",
				MavenEmbedderService.class, (spec) -> {
					spec.getParameters().getWorkingDirectory().set( project.getLayout().getProjectDirectory() );
					spec.getParameters().getMavenLocalDirectory().set( dsl.getLocalRepositoryDirectory() );
				}
		);

		final Provider<Directory> descriptorsDir = project.getLayout().getBuildDirectory().dir( "generated/sources/plugin-descriptors" );

		final TaskProvider<MavenPluginDescriptorTask> generatePluginDescriptorTask = project.getTasks().register( "generatePluginDescriptor", MavenPluginDescriptorTask.class, (task) -> {
			task.setGroup( "maven embedder" );

			task.getMavenEmbedderService().set( embedderServiceProvider );
			task.usesService( embedderServiceProvider );

			task.getDescriptorDirectory().set( descriptorsDir );
			mainSourceSet.getResources().srcDir( task.getDescriptorDirectory() );

			task.dependsOn( "compileJava" );
		} );

		project.getTasks().named( "jar", (jarTask) -> jarTask.dependsOn( generatePluginDescriptorTask ) );
	}
}
