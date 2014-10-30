Augmentor 
==============
[![Build Status](https://travis-ci.org/saladinkzn/augmentor.svg?branch=master)](https://travis-ci.org/saladinkzn/augmentor)
[![License](http://img.shields.io/badge/license-MIT-47b31f.svg)](#copyright-and-license)

Gradle task for augmenting another tasks to be run by enter click or in future by source changes. 
It can be useful if you develop project like Java library or Android application and want to simplify your build cycle.

Getting started
------------------
Add following snippet to enable creation of AugmentorTask
```groovy
buildscript {
	repositories {
        maven { url 'http://oss.jfrog.org/artifactory/oss-snapshot-local' }
	}

	dependencies {
        classpath 'ru.shadam.augmentor:augmentor:0.1-SNAPSHOT'
	}
}
```

Usage
------------------
Add following snippet into your build.gradle:

```groovy
task re-build(type: ru.shadam.augmentor.AugmentorTask) {
	innerTask = 'build' // name of gradle augmentor task
	// Optional parameters:
	scanInterval = 10 // watch interval, 0 to disable
	srcDirs = [ file('src/main/java') ] // list of directories to watch, project.sourceSets.main by default
}
```

Then, you can run this task by executing `gradlew re-build` command.

Future plans
-------------------
* Recompile and restart on source changes

Copyright and License
----------------------

Copyright 2014 (c) Timur Shakurov

Augmentor is licensed under [MIT license](LICENSE).
