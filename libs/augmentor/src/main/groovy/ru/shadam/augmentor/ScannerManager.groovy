package ru.shadam.augmentor

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

/**
 * @author sala
 */
class ScannerManager {
  private static final Logger logger = Logging.getLogger(ScannerManager)

  static class EventData {
    List<Path> createdOrModified
    List<Path> deleted

    EventData(List<Path> createdOrModified, List<Path> deleted) {
      this.createdOrModified = createdOrModified
      this.deleted = deleted
    }
  }

  static class FileTreeRegister extends SimpleFileVisitor<Path> {
    private WatchService watchService

    FileTreeRegister(WatchService watchService) {
      this.watchService = watchService
    }

    @Override
    FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      println "Registering $dir"
      dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
      return FileVisitResult.CONTINUE
    }
  }

  protected volatile boolean stopped
  protected WatchService watchService
  //
  protected def createdOrModified = []
  protected def deleted = []
  //
  int scanInterval
  Closure<EventData> callback

  ScannerManager(List<Path> paths, Closure<EventData> callback) {
    this.watchService = FileSystems.getDefault().newWatchService()
    this.callback = callback
    paths.each {
      Files.walkFileTree(it, new FileTreeRegister(watchService))
    }
  }

  def start() {
    stopped = false
    Thread.start {
      logger.debug 'ScannerManager started'
      while (!stopped) {
        WatchKey key = watchService.poll(1, TimeUnit.SECONDS)
        if(key != null) {
          def events = key.pollEvents()
          events.each {
            if (it.kind() == ENTRY_CREATE) {
              Path parentPath = (Path)key.watchable()
              def resolved = parentPath.resolve((Path)it.context())
              def file = resolved.toFile()
              if(file.isDirectory()) {
                // TODO extract method
                Files.walkFileTree(resolved, new FileTreeRegister(watchService))
                // TODO: we should collect files here
              } else if (file.isFile()) {
                createdOrModified.add(resolved)
              }
            } else if (it.kind() == ENTRY_MODIFY) {
              Path parentPath = (Path)key.watchable()
              def resolved = parentPath.resolve((Path)it.context())
              createdOrModified.add(resolved)
            } else if (it.kind() == ENTRY_DELETE) {
              Path parentPath = (Path)key.watchable()
              def resolved = parentPath.resolve((Path)it.context())
              deleted.add(resolved)
            }
          }
          key.reset()
          if(createdOrModified || deleted) {
            raiseEvent()
          }
        }
      }
      logger.debug 'ScannerManager stopped'
    }
  }

  protected def raiseEvent() {
    if(throttle()) {
      createdOrModified = []
      deleted = []
      callback.call(new EventData(createdOrModified, deleted))
    }
  }

  private Long lastFire

  private boolean throttle() {
    logger.debug('throttle() called')
    if(scanInterval == 0) {
      // In this case, event is fired on each file changed
      logger.debug('scanInterval == 0, no throttling')
      return true
    }
    if(lastFire == null) {
      logger.debug('lastFire == null, first call, no throttling')
      lastFire = System.currentTimeMillis()
      return true
    }
    if((System.currentTimeMillis() - lastFire) / 1000 > scanInterval) {
      logger.debug("lastFire: ${lastFire}, now: ${System.currentTimeMillis()}. Event fired. ")
      lastFire = System.currentTimeMillis()
      return true
    }
    logger.debug('event was not fired')
    return false
  }

  def stop() {
    stopped = true
  }
}
