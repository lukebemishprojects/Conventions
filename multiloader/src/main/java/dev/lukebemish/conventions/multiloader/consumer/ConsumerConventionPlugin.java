package dev.lukebemish.conventions.multiloader.consumer;

import dev.lukebemish.conventions.multiloader.MultiloaderExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.util.List;
import java.util.Map;

public class ConsumerConventionPlugin implements Plugin<Project> {
	@SuppressWarnings("UnstableApiUsage")
	@Override
	public void apply(Project project) {
		MultiloaderExtension multiloader = project.getExtensions().getByType(MultiloaderExtension.class);

		project.getPluginManager().apply("dev.lukebemish.conventions.multiloader.shared");

		Configuration commonJava = project.getConfigurations().maybeCreate("commonJava");
		commonJava.setCanBeResolved(true);
		commonJava.setCanBeConsumed(false);
		Configuration commonResources = project.getConfigurations().maybeCreate("commonResources");
		commonResources.setCanBeResolved(true);
		commonResources.setCanBeConsumed(false);

		DependencyCollector collectorCompileOnly = project.getObjects().dependencyCollector();
		collectorCompileOnly.add(project.getDependencies().project(Map.of("path", multiloader.commonPath)), dep -> {
			((ModuleDependency) dep).capabilities(caps -> {
				caps.requireCapability(project.getGroup()+":"+project.project(":").getName());
			});
		});
		project.getConfigurations().getByName("compileOnly").fromDependencyCollector(collectorCompileOnly);

		project.getDependencies().add("commonJava", project.getDependencies().project(Map.of(
			"path", multiloader.commonPath,
			"configuration", "commonJava"
		)));

		project.getDependencies().add("commonResources", project.getDependencies().project(Map.of(
			"path", multiloader.commonPath,
			"configuration", "commonResources"
		)));

		project.getTasks().named("compileJava", JavaCompile.class, it -> {
			it.dependsOn(commonJava);
			it.source(commonJava);
		});

		project.getTasks().named("processResources", ProcessResources.class, it -> {
			it.dependsOn(commonResources);
			it.from(commonResources);
		});

		project.getTasks().named("javadoc", Javadoc.class, it -> {
			it.dependsOn(commonJava);
			it.source(commonJava);
		});

		project.getTasks().named("sourcesJar", Jar.class, it -> {
			it.dependsOn(commonJava);
			it.from(commonJava);
			it.dependsOn(commonResources);
			it.from(commonResources);
		});

		for (String variantName : List.of("apiElements", "runtimeElements", "sourcesElements", "javadocElements")) {
			project.getConfigurations().getByName(variantName).getOutgoing().capability(
				project.getGroup() + ":" + project.project(":").getName() + ":" + project.getVersion()
			);
		}
	}
}
