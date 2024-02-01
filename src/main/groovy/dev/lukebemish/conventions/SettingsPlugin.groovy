package dev.lukebemish.conventions

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.dsl.VersionCatalogBuilder
import org.gradle.caching.configuration.BuildCacheConfiguration
import org.gradle.caching.http.HttpBuildCache
import org.gradle.caching.http.HttpBuildCacheCredentials

@CompileStatic
class SettingsPlugin implements Plugin<Settings> {
    @Override
    void apply(Settings settings) {
        settings.plugins.apply('org.gradle.toolchains.foojay-resolver-convention')
        settings.plugins.apply('com.gradle.enterprise')

        settings.dependencyResolutionManagement {
            it.versionCatalogs { container ->
                container.named('libs') {
                    Versioning.apply(it as VersionCatalogBuilder)
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

        settings.extensions.getByType(BuildCacheConfiguration).tap {
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
