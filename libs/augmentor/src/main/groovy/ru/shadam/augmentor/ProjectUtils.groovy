package ru.shadam.augmentor

import org.gradle.api.Project

/**
 * @author sala
 */
class ProjectUtils {
  static Collection<File> getSourcePaths(Project project) {
    if(project.hasProperty('sourceSets')) {
      return project.sourceSets.main.allSource.srcDirs
    } else {
      return null
    }
  }
}
