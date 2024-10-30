package org.hibernate.build.maven.embedder;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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

		final Configuration embedderDependencies = project.getConfigurations().maybeCreate( "embedder" );

//		final Configuration embedderDependencies = project.getConfigurations().detachedConfiguration(
//				project.getDependencies().create( "org.slf4j:slf4j-api:2.0.16" ),
//				project.getDependencies().create( "org.slf4j:slf4j-reload4j:2.0.16" )
//		);

		final MavenEmbedderConfig dsl = project.getExtensions().create(
				"mavenEmbedder",
				MavenEmbedderConfig.class
		);

		final Provider<MavenEmbedderService> embedderServiceProvider = sharedServices.registerIfAbsent(
				"maven-embedder",
				MavenEmbedderService.class, (spec) -> {
					spec.getParameters().getWorkingDirectory().set( project.getLayout().getProjectDirectory() );
					spec.getParameters().getMavenLocalDirectory().set( dsl.getLocalRepositoryDirectory() );
					spec.getParameters().getEmbedderDependencies().from( embedderDependencies );
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

		project.afterEvaluate( project1 -> {
			project.getLogger().lifecycle( "################################################" );
			project.getLogger().lifecycle( "`embedder` dependency files -" );
			for ( File file : embedderDependencies.getFiles() ) {
				project.getLogger().lifecycle( "    - " + file.getName() );
			}
			project.getLogger().lifecycle( "################################################" );

			project.getLogger().lifecycle( "################################################" );
			project.getLogger().lifecycle( "`embedder` dependency files -" );
			embedderDependencies.forEach( (file) -> {
				project.getLogger().lifecycle( "    - " + file.getName() );
			} );
			project.getLogger().lifecycle( "################################################" );
		} );
	}
}
