package org.hibernate.build.maven.embedder;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import org.jetbrains.annotations.NotNull;

/**
 * @author Steve Ebersole
 */
public abstract class MavenEmbedderService implements BuildService<MavenEmbedderService.Config> {
	public static final String EMBEDDER_CLASS_NAME = "org.apache.maven.cli.MavenCli";

	interface Config extends BuildServiceParameters {
		DirectoryProperty getWorkingDirectory();
		DirectoryProperty getMavenLocalDirectory();
		ConfigurableFileCollection getEmbedderDependencies();
	}

	private final MavenEmbedderDelegate embedderDelegate;

	public MavenEmbedderService() {
		System.setProperty( "maven.multiModuleProjectDirectory", getParameters().getWorkingDirectory().toString() );

		final Class<?> delegateClass = loadDelegateClass( getParameters().getEmbedderDependencies() );
		this.embedderDelegate = new MavenEmbedderDelegate(
				getParameters().getWorkingDirectory().get().getAsFile(),
				getParameters().getMavenLocalDirectory().get().getAsFile(),
				delegateClass
		);
	}

	public MavenEmbedderDelegate getEmbedderDelegate() {
		return embedderDelegate;
	}

	private static Class<?> loadDelegateClass(ConfigurableFileCollection embedderDependenciesCollection) {
		System.out.println( "################################################" );
		System.out.println( "`embedder` dependency files (from service) -" );
		for ( File file : embedderDependenciesCollection.getFiles() ) {
			System.out.println( "    - " + file.getName() + " (exists - " + file.exists() + ")" );
		}
		System.out.println( "################################################" );

		final URL[] urls = collectDependencyUrls( embedderDependenciesCollection );

		try (URLClassLoader urlClassLoader = new URLClassLoader( urls )) {
			return urlClassLoader.loadClass( EMBEDDER_CLASS_NAME );
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to load Maven Embedder class", e );
		}
	}

	private static URL @NotNull [] collectDependencyUrls(ConfigurableFileCollection embedderDependenciesCollection) {
		final Set<File> embedderDependencies = embedderDependenciesCollection.getFiles();
		final URL[] urls = new URL[embedderDependencies.size()];

		try {
			int position = 0;
			for ( File embedderDependency : embedderDependencies ) {
				urls[position++] = embedderDependency.toURL();
			}
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to process embedder dependencies for loading Maven Embedder class", e );
		}
		return urls;
	}

	public static class MavenEmbedderDelegate {
		private final File workingDir;
		private final File mavenLocalRepoDir;

		private final Object embedder;
		private final Method executeMethod;

		public MavenEmbedderDelegate(File workingDir, File mavenLocalRepoDir, Class<?> embedderClass) {
			this.workingDir = workingDir;
			this.mavenLocalRepoDir = mavenLocalRepoDir;

			this.embedder = createEmbedder( embedderClass );
			this.executeMethod = resolveExecuteMethod( embedderClass );
		}

		public void execute(String... tasksAndArgs) {
			final List<String> cml = new ArrayList<>();
			Collections.addAll( cml, tasksAndArgs );

			cml.add( "-Dmaven.repo.local=" + mavenLocalRepoDir.getAbsolutePath() );
			cml.add( "--debug" );

			final String workingDirectoryPath = workingDir.getAbsolutePath();

			try {
				final int resultCode = (int) executeMethod.invoke(
						embedder,
						cml.toArray(new String[0]),
						workingDirectoryPath,
						System.out,
						System.err
				);
				if ( resultCode != 0 ) {
					throw new RuntimeException( "Maven Embedder failed with code `" + resultCode + "`" );
				}
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to invoke Maven Embedder", e );
			}
		}
	}

	private static Object createEmbedder(Class<?> embedderClass) {
		final Constructor<?> constructor = resolveEmbedderConstructor( embedderClass );
		return createEmbedder( constructor );
	}

	private static Constructor<?> resolveEmbedderConstructor(Class<?> embedderClass) {
		try {
			return embedderClass.getConstructor();
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to create Maven Embedder delegate (find constructor)", e );
		}
	}

	private static Object createEmbedder(Constructor<?> constructor) {
		try {
			return constructor.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to create Maven Embedder delegate (call constructor)", e );
		}
	}

	private static Method resolveExecuteMethod(Class<?> embedderClass) {
		try {
			return embedderClass.getDeclaredMethod( "doMain", String[].class, String.class, PrintStream.class, PrintStream.class );
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException( "Unable to locate MavenCli#doMain method", e );
		}
	}
}
