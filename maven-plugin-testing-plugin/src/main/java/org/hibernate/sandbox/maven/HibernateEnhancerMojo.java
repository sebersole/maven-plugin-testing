package org.hibernate.sandbox.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @author Steve Ebersole
 */
@Mojo(name = "hibernate-enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class HibernateEnhancerMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	public void execute() throws MojoExecutionException, MojoFailureException {
		// todo : the crux of the maven "plugin" for applying enhancement
		new Enhancer().enhance( null );
	}
}
