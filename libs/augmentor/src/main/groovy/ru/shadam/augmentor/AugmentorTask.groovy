package ru.shadam.augmentor
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.*

import java.nio.file.Path

/**
 * @author sala
 */
class AugmentorTask extends DefaultTask {
  String innerTask
  int scanInterval = 0
  List<File> scanDirs
  volatile boolean finished = true

  @TaskAction
  void augment() {
    // TODO: gradle version check
    assert innerTask != null, 'innerTask cannot be null'

    def cancellationTokenSource = runTask()
    println 'Press any key to re-run task or \'q\' or \'Q\' to stop'
    ScannerManager scannerManager = null
    if(scanInterval != -1) {
      def list = [] as List<Path>
      def dirsToScan
      if(scanDirs) {
        dirsToScan = scanDirs.collect { it.toPath() }
      } else {
        dirsToScan = ProjectUtils.getSourcePaths(project)?.collect { it.toPath() }
      }
      if(dirsToScan == null) {
        logger.warn("Cannot resolve dirsToScan. Your task neither specified sourceSets nor provided srcDirs property")
      } else {
        list.addAll(dirsToScan)
        scannerManager = new ScannerManager(list, { ScannerManager.EventData ed ->
          cancellationTokenSource.cancel()
          println 'Waiting for task finish'
          while (!finished) {
            Thread.sleep(10)
          }
          cancellationTokenSource = owner.runTask()
        })
        scannerManager.scanInterval = scanInterval
        scannerManager.start()
      }
    }
    infinite:
    while (true) {
      while (System.in.available() > 0) {
        def input = System.in.read()

        if (input >= 0) {
          char c = input as char
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
    }
    scannerManager?.stop()
  }


  protected static ProjectConnection createConnection(GradleConnector gradleConnector, Project project) {
    gradleConnector
            .forProjectDirectory(project.projectDir)
            .useInstallation(project.gradle.gradleHomeDir)
            .connect()
  }

  protected CancellationTokenSource runTask() {

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
