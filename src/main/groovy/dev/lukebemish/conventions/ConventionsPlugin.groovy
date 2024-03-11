package dev.lukebemish.conventions

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
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

	}

	static void applySettings(Settings settings) {
		settings.pluginManager.apply('org.gradle.toolchains.foojay-resolver-convention')
		settings.pluginManager.apply('com.gradle.enterprise')

		settings.pluginManagement.resolutionStrategy {
			it.eachPlugin {
				if (it.requested.id.name.startsWith('dev.lukebemish.conventions')) {
					it.useVersion(VERSION)
				}
			}
		}

		String vcNotation = "dev.lukebemish:conventions:${VERSION}"
		settings.dependencyResolutionManagement { deps ->
			deps.versionCatalogs { container ->
				container.maybeCreate('libs').tap {
					from(vcNotation)
				}
			}
		}

		settings.extensions.getByType(GradleEnterpriseExtension).tap {
			if (System.getenv('CI') != null) {
				buildScan {
					it.publishAlways()
					it.termsOfServiceUrl = "https://gradle.com/terms-of-service"
					it.termsOfServiceAgree = "yes"
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
					if (settings.providers.gradleProperty('buildCachePush').orNull) {
						it.push = true
					} else {
						it.push = false
					}
				}
			} else if (System.getenv('BUILD_CACHE_URL')) {
				remote(HttpBuildCache) {
					it.url = System.getenv('BUILD_CACHE_URL')
					it.credentials { HttpBuildCacheCredentials credentials ->
						credentials.username = System.getenv('BUILD_CACHE_USER')
						credentials.password = System.getenv('BUILD_CACHE_PASSWORD')
					}
					if (System.getenv('BUILD_CACHE_PUSH') || System.getenv('CI')) {
						it.push = true
					} else {
						it.push = false
					}
				}
			}
		}
	}
}
