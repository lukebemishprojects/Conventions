package dev.lukebemish.conventions

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.dsl.VersionCatalogBuilder
import org.gradle.caching.configuration.BuildCacheConfiguration
import org.gradle.caching.http.HttpBuildCache

@CompileStatic
class SettingsPlugin implements Plugin<Settings> {
    @Override
    void apply(Settings settings) {
        settings.plugins.apply('org.gradle.toolchains.foojay-resolver-convention')
        settings.plugins.apply('com.gradle.enterprise')

        settings.dependencyResolutionManagement {
            versionCatalogs {
                libs {
                    Versioning.apply(it as VersionCatalogBuilder)
                }
            }
        }

        settings.extensions.getByType(GradleEnterpriseExtension).tap {
            if (System.getenv('CI') != null) {
                buildScan {
                    publishAlways()
                    termsOfServiceUrl = "https://gradle.com/terms-of-service"
                    termsOfServiceAgree = "yes"
                }
            }
        }

        settings.extensions.getByType(BuildCacheConfiguration).tap {
            if (settings.providers.gradleProperty('buildCacheUrl').orNull) {
                remote(HttpBuildCache) {
                    url = settings.providers.gradleProperty('buildCacheUrl').orNull
                    credentials {
                        username = settings.providers.gradleProperty('buildCacheUser').orNull
                        password = settings.providers.gradleProperty('buildCachePassword').orNull
                    }
                    if (settings.providers.gradleProperty('buildCachePush').orNull) {
                        push = true
                    } else {
                        push = false
                    }
                }
            } else if (System.getenv('BUILD_CACHE_URL')) {
                remote(HttpBuildCache) {
                    url = System.getenv('BUILD_CACHE_URL')
                    credentials {
                        username = System.getenv('BUILD_CACHE_USER')
                        password = System.getenv('BUILD_CACHE_PASSWORD')
                    }
                    if (System.getenv('BUILD_CACHE_PUSH') || System.getenv('CI')) {
                        push = true
                    } else {
                        push = false
                    }
                }
            }
        }
    }
}
