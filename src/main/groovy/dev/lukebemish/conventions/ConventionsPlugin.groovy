package dev.lukebemish.conventions

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.caching.http.HttpBuildCache
import org.gradle.caching.http.HttpBuildCacheCredentials

@CompileStatic
abstract class ConventionsPlugin implements Plugin<Object> {
	public static final String VERSION = ConventionsPlugin.class.getPackage().getImplementationVersion()

	@Override
	void apply(Object target) {
		if (target instanceof Project) {
			applyProject(target)
		} else if (target instanceof Settings) {
			applySettings(target)
		} else {
			throw new IllegalArgumentException("Unsupported target type: ${target.getClass().name}")
		}
	}

	static void applyProject(Project project) {
		addRepositories(project.getRepositories(), false)
	}

	static void applySettings(Settings settings) {
		settings.pluginManager.apply('org.gradle.toolchains.foojay-resolver-convention')
		settings.pluginManager.apply('com.gradle.develocity')

		settings.pluginManagement.resolutionStrategy {
			it.eachPlugin {
				if (it.requested.id.name.startsWith('dev.lukebemish.conventions')) {
					it.useVersion(VERSION)
				}
			}
		}

		String vcNotation = "dev.lukebemish:conventions:${VERSION}"
		settings.dependencyResolutionManagement { deps ->
			deps.repositories { repositories ->
				addRepositories(repositories, false)
			}
			deps.versionCatalogs { container ->
				container.maybeCreate('cLibs').tap {
					from(vcNotation)
				}
			}
		}

		def isCI = !settings.providers.environmentVariable('CI').orElse('').get().isEmpty()

		settings.extensions.getByType(DevelocityConfiguration).tap {
			it.buildScan {
				it.uploadInBackground.set(false)
				it.publishing.onlyIf { isCI }
				if (isCI) {
					it.termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
					it.termsOfUseAgree.set("yes")
				}
				it.capture {
					it.buildLogging.set(false)
				}
			}
		}

		settings.getBuildCache().tap {
			if (settings.providers.gradleProperty('buildCacheUrl').orNull) {
				remote(HttpBuildCache) {
					it.url = settings.providers.gradleProperty('buildCacheUrl').orNull
					it.credentials { HttpBuildCacheCredentials credentials ->
						credentials.username = settings.providers.gradleProperty('buildCacheUser').orNull
						credentials.password = settings.providers.gradleProperty('buildCachePassword').orNull
					}
					it.push = isCI
				}
			} else if (System.getenv('BUILD_CACHE_URL')) {
				remote(HttpBuildCache) {
					it.url = System.getenv('BUILD_CACHE_URL')
					it.credentials { HttpBuildCacheCredentials credentials ->
						credentials.username = System.getenv('BUILD_CACHE_USER')
						credentials.password = System.getenv('BUILD_CACHE_PASSWORD')
					}
					it.push = isCI
				}
			}
		}
	}

	static void addRepositories(RepositoryHandler repositories, boolean plugins) {
		repositories.maven { MavenArtifactRepository m ->
			m.name = "Luke's Maven"
			m.url = "https://maven.lukebemish.dev/releases/"
		}
		if (VERSION.contains("-pr")) {
			repositories.maven { MavenArtifactRepository m ->
				m.name = "Luke's Pull Request Maven"
				m.url = "https://maven.lukebemish.dev/pullrequests/"
			}
		} else if (VERSION.endsWith("-SNAPSHOT")) {
			repositories.maven { MavenArtifactRepository m ->
				m.name = "Luke's Snapshot Maven"
				m.url = "https://maven.lukebemish.dev/snapshots/"
			}
		} else if (VERSION.endsWith("-dirty")) {
			repositories.mavenLocal()
		}
		if (plugins) {
			repositories.gradlePluginPortal()
		}
	}
}
