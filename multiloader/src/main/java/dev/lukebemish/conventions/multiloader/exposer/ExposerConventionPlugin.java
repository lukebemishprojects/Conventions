package dev.lukebemish.conventions.multiloader.exposer;

import dev.lukebemish.conventions.multiloader.MultiloaderExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;

public class ExposerConventionPlugin implements Plugin<Project> {
	public void apply(Project project) {
		MultiloaderExtension multiloader = project.getExtensions().getByType(MultiloaderExtension.class);

		project.getPluginManager().apply("dev.lukebemish.conventions.multiloader.shared");

		Configuration commonJava = project.getConfigurations().maybeCreate("commonJava");
		commonJava.setCanBeResolved(false);
		commonJava.setCanBeConsumed(true);
		Configuration commonResources = project.getConfigurations().maybeCreate("commonResources");
		commonResources.setCanBeResolved(false);
		commonResources.setCanBeConsumed(true);

		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		SourceSet main = sourceSets.getByName("main");

		for (File dir : main.getJava().getSrcDirs()) {
			project.getArtifacts().add(commonJava.getName(), dir);
		}

		for (File dir : main.getResources().getSrcDirs()) {
			project.getArtifacts().add(commonResources.getName(), dir);
		}
	}
}
