package convention.versioning

import dev.lukebemish.managedversioning.ManagedVersioningExtension
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class VersioningPlugin implements Plugin<Settings> {
	@Override
	void apply(Settings settings) {
		settings.getPluginManager().apply('dev.lukebemish.managedversioning')

		settings.extensions.getByType(ManagedVersioningExtension).tap {
			versionFile.set settings.layout.rootDirectory.file('version.properties')
			gitWorkingDir.set settings.layout.rootDirectory
			versionPRs()
			versionSnapshots()

			publishing.mavenPullRequest()
			publishing.mavenRelease()

			gitHubActions {
				it.register('release') {
					it.prettyName.set 'Release'
					it.workflowDispatch.set(true)
					it.gradleJob {
						it.name.set 'build'
						it.javaVersion.set '21'
						it.step {
							it.setupGitUser()
						}
						it.buildCache()
						it.readOnly.set false
						it.gradlew 'Tag Release', 'tagRelease'
						it.gradlew 'Build', 'build'
						it.step {
							it.run.set 'git push && git push --tags'
						}
						it.gradlew 'Publish', 'publish'
						it.mavenRelease('github')
					}
				}
				it.register('build_pr') {
					it.prettyName.set 'Build PR'
					it.pullRequest.set(true)
					it.gradleJob {
						it.name.set 'build'
						it.javaVersion.set '21'
						it.gradlew 'Build', 'build'
						it.gradlew 'Publish', 'publish'
						it.pullRequestArtifact()
					}
				}
				it.register('publish_pr') {
					it.prettyName.set 'Publish PR'
					it.publishPullRequestAction(
						'github',
						'dev/lukebemish/conventions/conventions-*',
						'Build PR'
					)
				}
			}
		}
	}
}
