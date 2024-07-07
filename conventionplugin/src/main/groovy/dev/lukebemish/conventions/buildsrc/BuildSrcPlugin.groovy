package dev.lukebemish.conventions.buildsrc

import dev.lukebemish.conventions.ConventionsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class BuildSrcPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply(ConventionsPlugin)
        project.pluginManager.apply('groovy-gradle-plugin')

        ConventionsPlugin.addRepositories(project.repositories, true)
    }
}
