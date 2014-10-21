Augmentor 
==============
[![Build Status](https://travis-ci.org/saladinkzn/augmentor.svg?branch=master)](https://travis-ci.org/saladinkzn/augmentor)
[![License](http://img.shields.io/badge/license-MIT-47b31f.svg)](#copyright-and-license)

Gradle task for augmenting another tasks to be run by enter click or in future by source changes. 
It can be useful if you develop project like Java library or Android application and want to simplify your build cycle.

Getting started
------------------
TBD: Will be updated when lib will be published to jfrog/bintray

Usage
------------------
Add following snippet into your build.gradle:

```groovy
task re-build(type: ru.shadam.augmentor.Augmentor) {
	innerTask = 'build'
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
