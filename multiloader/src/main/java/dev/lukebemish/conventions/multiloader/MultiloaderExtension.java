package dev.lukebemish.conventions.multiloader;

import org.gradle.api.Action;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

import javax.inject.Inject;
import java.util.List;

public abstract class MultiloaderExtension {
	public final String minecraftVersion;
	public final boolean publishPRs;
	public final boolean publishSnapshots;
	public final boolean publishCentral;
	public final Action<JavaToolchainSpec> toolchainAction;
	public final String githubRepo;
	public final String license;
	public final String modId;
	public final String modName;
	public final String modDescription;
	public final String modAuthor;
	public final String commonPath;

	@Inject
	public MultiloaderExtension(MultiloaderSettingsExtension extension) {
		this.minecraftVersion = extension.getMinecraftVersion().get();
		publishSnapshots = extension.getPublishSnapshots().get();
		publishPRs = extension.getPublishPRs().get();
		publishCentral = extension.getPublishCentral().get();
		List<Action<JavaToolchainSpec>> actions = extension.getToolchainActions().get();
		toolchainAction = spec -> {
			for (Action<JavaToolchainSpec> action : actions) {
				action.execute(spec);
			}
		};
		githubRepo = extension.getGithubRepo().get();
		license = extension.getLicense().get();
		modId = extension.getModId().get();
		modName = extension.getModName().get();
		modDescription = extension.getModDescription().get();
		modAuthor = extension.getModAuthor().get();
		commonPath = extension.getCommonPath().get();
	}
}
