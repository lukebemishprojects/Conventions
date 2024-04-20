package dev.lukebemish.conventions.multiloader;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

public class MultiloaderConventionsPlugin implements Plugin<Settings> {
	@Override
	public void apply(Settings settings) {
		settings.getPluginManager().apply("dev.lukebemish.conventions");
		MultiloaderSettingsExtension extension = settings.getExtensions().create("multiloader", MultiloaderSettingsExtension.class, settings);
		settings.getGradle().settingsEvaluated(s -> {
			s.dependencyResolutionManagement(deps -> {
				deps.versionCatalogs(vcs -> {
					var libs = vcs.maybeCreate("libs");
					libs.version("minecraft", extension.getMinecraftVersion().get());
					libs.library("minecraft", "com.mojang", "minecraft").versionRef("minecraft");
				});
			});
		});
	}
}
