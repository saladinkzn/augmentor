package ru.shadam.augmentor

import org.gradle.api.Project

/**
 * @author sala
 */
class ProjectUtils {
  static Collection<File> getSourcePaths(Project project) {
    project.sourceSets.main.allSource.srcDirs
  }
}
