package dev.lukebemish.conventions.multiloader;

import dev.lukebemish.conventions.ConventionsPlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MultiloaderSettingsExtension {
	public abstract Property<String> getMinecraftVersion();
	public abstract Property<Boolean> getPublishPRs();
	public abstract Property<Boolean> getPublishSnapshots();
	public abstract Property<Boolean> getPublishCentral();
	public abstract ListProperty<Action<JavaToolchainSpec>> getToolchainActions();

	public abstract Property<String> getGithubRepo();
	public abstract Property<String> getLicense();
	public abstract Property<String> getModId();
	public abstract Property<String> getModName();
	public abstract Property<String> getModDescription();
	public abstract Property<String> getModAuthor();
	public abstract Property<String> getCommonPath();

	private final Map<String, List<Action<Project>>> projectActions = new HashMap<>();
	private final Settings settings;

	public MultiloaderSettingsExtension(Settings settings) {
		this.settings = settings;
		this.getPublishPRs().convention(false);
		this.getPublishSnapshots().convention(false);
		this.getPublishCentral().convention(false);

		this.getGithubRepo().convention(settings.getProviders().provider(() -> settings.getRootProject().getName()).orElse(settings.getProviders().gradleProperty("github_repo")));
		this.getLicense().convention(settings.getProviders().gradleProperty("license"));
		this.getModId().convention(settings.getProviders().gradleProperty("mod_id"));
		this.getModName().convention(settings.getProviders().gradleProperty("mod_name"));
		this.getModDescription().convention(settings.getProviders().gradleProperty("mod_description"));
		this.getModAuthor().convention(settings.getProviders().gradleProperty("mod_author"));

		settings.getGradle().beforeProject(p -> {
			List<Action<Project>> actions = projectActions.get(p.getPath());
			if (actions != null) {
				p.getExtensions().create("multiloader", MultiloaderExtension.class, this);
				for (Action<Project> action : actions) {
					action.execute(p);
				}
			}
		});
	}

	public void toolchain(Action<JavaToolchainSpec> action) {
		getToolchainActions().add(action);
	}

	public void common(String project) {
		common(project, p -> {});
	}
	public void common(String project, Action<Project> work) {
		exposer(project, p -> {
			//p.getPluginManager().apply("dev.lukebemish.conventions.multiloader.common");
			work.execute(p);
		});
	}

	public void exposer(String project) {
		exposer(project, p -> {});
	}
	public void exposer(String project, Action<Project> work) {
		getCommonPath().set(project);
		addPlugin("dev.lukebemish.conventions.multiloader.exposer");
		shared(project, p -> {
			p.getPluginManager().apply("dev.lukebemish.conventions.multiloader.exposer");
			work.execute(p);
		});
	}

	public void shared(String project) {
		shared(project, p -> {});
	}
	public void shared(String project, Action<Project> work) {
		addPlugin("dev.lukebemish.conventions.multiloader.shared");
		setup(project, p -> {
			p.getPluginManager().apply("dev.lukebemish.conventions.multiloader.shared");
			work.execute(p);
		});
	}

	public void consumer(String project) {
		consumer(project, p -> {});
	}
	public void consumer(String project, Action<Project> work) {
		addPlugin("dev.lukebemish.conventions.multiloader.consumer");
		shared(project, p -> {
			p.getPluginManager().apply("dev.lukebemish.conventions.multiloader.consumer");
			work.execute(p);
		});
	}

	public void setup(String project) {
		setup(project, p -> {});
	}
	public void setup(String project, Action<Project> work) {
		projectActions.computeIfAbsent(project, k -> new ArrayList<>()).add(work);
	}

	private void addPlugin(String id) {
		settings.getPluginManagement().getPlugins().id(id).version(ConventionsPlugin.VERSION).apply(false);
	}
}
