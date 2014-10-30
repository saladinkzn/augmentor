package ru.shadam.augmentor

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

  volatile boolean stopped
  WatchService watchService
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
      while (!stopped) {
        WatchKey key = watchService.poll(15, TimeUnit.SECONDS)
        if(key != null) {
          def events = key.pollEvents()
          def createdOrModified = []
          def deleted = []
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
          callback.call(new EventData(createdOrModified, deleted))
          key.reset()
        }
      }
    }
  }

  def stop() {
    stopped = true
  }
}
