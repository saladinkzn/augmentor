package ru.shadam.augmentor
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.*
/**
 * @author sala
 */
class AugmentorTask extends DefaultTask {
  String innerTask
  volatile boolean finished = true

  @TaskAction
  void augment() {
    // TODO: gradle version check

    def cancellationTokenSource = runTask()
    infinite:
    while (true) {
      while (System.in.available() > 0) {
        def input = System.in.read()

        if (input >= 0) {
          char c = (char) input
          if (c == 'q' || c == 'Q') {
            break infinite
          } else {
            cancellationTokenSource.cancel()
            println 'Waiting for task finish'
            while (!finished) {
              Thread.sleep(10)
            }
            cancellationTokenSource = runTask()
            while (System.in.available() > 0) {
              int available = System.in.available()
              for (int i = 0; i < available; i++) {
                if (System.in.read() == -1) {
                  break
                }
              }
            }
          }
        }
      }
      Thread.sleep(500)

      println 'Press any key to continue'
      System.in.read()
    }
  }


  protected static ProjectConnection createConnection(GradleConnector gradleConnector, Project project) {
    gradleConnector
            .forProjectDirectory(project.projectDir)
            .useInstallation(project.gradle.gradleHomeDir)
            .connect()
  }

  private CancellationTokenSource runTask() {

    if(!finished) {
      throw new IllegalStateException("Trying to simultaneously start 2 tasks")
    }
    final GradleConnector connector = GradleConnector.newConnector()
    final CancellationTokenSource cancellationTokenSource = connector.newCancellationTokenSource()
    final ProjectConnection connection = createConnection(connector, project)
    Thread.start {
      try {
        finished = false
        def cancellationToken = cancellationTokenSource.token()
        BuildLauncher buildLauncher = connection.newBuild()
        buildLauncher.withCancellationToken(cancellationToken)
        buildLauncher.forTasks(innerTask).run(new ResultHandler<Void>() {
          @Override
          void onComplete(Void aVoid) {
            finished = true
          }

          @Override
          void onFailure(GradleConnectionException e) {
            finished = true
          }
        })
      } finally {
        connection.close()
      }
    }
    return cancellationTokenSource
  }
}
