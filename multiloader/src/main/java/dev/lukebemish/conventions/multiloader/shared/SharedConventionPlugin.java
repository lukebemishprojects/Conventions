package dev.lukebemish.conventions.multiloader.shared;

import dev.lukebemish.conventions.multiloader.MultiloaderExtension;
import dev.lukebemish.managedversioning.ManagedVersioningExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.plugins.signing.SigningExtension;

import java.util.List;
import java.util.Map;

public class SharedConventionPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		MultiloaderExtension multiloader = project.getExtensions().getByType(MultiloaderExtension.class);

		project.getPluginManager().apply("java-library");
		project.getPluginManager().apply("maven-publish");
		if (multiloader.publishCentral) {
			project.getPluginManager().apply("signing");
		}
		project.getPluginManager().apply("dev.lukebemish.managedversioning");
		project.getPluginManager().apply("dev.lukebemish.conventions.java");

		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		ManagedVersioningExtension managedVersioning = project.getExtensions().getByType(ManagedVersioningExtension.class);
		JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);

		managedVersioning.getVersionFile().set(project.getRootDir().toPath().resolve("version.properties").toFile());
		managedVersioning.getGitWorkingDir().set(project.getRootDir());
		managedVersioning.getMetadataVersion().set(multiloader.minecraftVersion);

		if (multiloader.publishPRs) {
			managedVersioning.versionPRs();
			managedVersioning.getPublishing().mavenPullRequest(publishing);
		}

		if (multiloader.publishSnapshots) {
			managedVersioning.versionSnapshots();
			managedVersioning.getPublishing().mavenSnapshot(publishing);
		}

		managedVersioning.apply();

		java.toolchain(multiloader.toolchainAction);
		java.withJavadocJar();
		java.withSourcesJar();

		// TODO: add convention option for this?
		project.getTasks().named("javadoc", Javadoc.class, task -> {
			task.include("**/api/**");
		});

		BasePluginExtension base = project.getExtensions().getByType(BasePluginExtension.class);
		base.getArchivesName().set(project.project(":").getName() + "-" + project.getName() + "-" + multiloader.minecraftVersion);

		project.getRepositories().mavenCentral();
		project.getRepositories().maven(maven -> {
			maven.setName("Architectury");
			maven.setUrl("https://maven.architectury.dev/");
		});
		project.getRepositories().maven(maven -> {
			maven.setName("ParchmentMC");
			maven.setUrl("https://maven.parchmentmc.org/");
		});

		String baseModuleName = project.project(":").getName() + "-" + project.getName();

		publishing.publications(publications -> {
			publications.register("mavenJava", MavenPublication.class, it -> {
				it.from(project.getComponents().getByName("java"));
				it.setArtifactId(baseModuleName);
				if (multiloader.publishCentral) {
					managedVersioning.getPublishing().sign(project.getExtensions().getByType(SigningExtension.class), it);
				}
				managedVersioning.getPublishing().pom(it, multiloader.githubRepo, multiloader.license);
				it.pom(pom -> {
					pom.getName().set(multiloader.modName + " - " + project.getName());
					pom.getDescription().set(multiloader.modDescription);
				});
			});
		});

		project.getTasks().named("sourcesJar", Jar.class, task -> {
			task.from(project.getRootDir() + "/LICENSE", spec -> {
				spec.rename(it -> it + "_" + project.project(":").getName());
			});
		});

		for (String variantName : List.of("apiElements", "runtimeElements", "sourcesElements", "javadocElements")) {
			project.getConfigurations().getByName(variantName).getOutgoing().capability(
				project.getGroup() + ":" + project.project(":").getName() + "-" + project.getName() + ":" + project.getVersion()
			);
			project.getConfigurations().getByName(variantName).getOutgoing().capability(
				project.getGroup() + ":" + project.project(":").getName() + ":" + project.getVersion()
			);
		}

		project.getTasks().named("jar", Jar.class, task -> {
			task.from(project.getRootDir() + "/LICENSE", spec -> {
				spec.rename(it -> it + "_" + project.project(":").getName());
			});

			task.getManifest().attributes(Map.of(
				"Specification-Title", multiloader.modName,
				"Specification-Vendor", multiloader.modAuthor,
				"Specification-Version", project.getVersion(),
				"Implementation-Title", multiloader.modName + " - " + project.getName(),
				"Implementation-Vendor", multiloader.modAuthor,
				"Implementation-Version", project.getVersion().toString(),
				"Implementation-Commit-Time", managedVersioning.getTimestamp().get(),
				"Implementation-Commit", managedVersioning.getHash().get(),
				"Built-On-Minecraft", multiloader.minecraftVersion
			));
		});
	}
}
