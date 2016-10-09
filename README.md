[![Build Status](https://travis-ci.org/yeputons/shell-fall-2016.svg?branch=master)](https://travis-ci.org/yeputons/shell-fall-2016)

# shell-fall-2016
Educational project from Fall 2016, implementation of a shell.


# Building with Maven

First, you should install <a href="https://maven.apache.org/">Maven</a>.
Note that this project is written in Kotlin, so Maven will have to download a lot other dependencies.
Available Maven targets:

* `mvn build` - build the project.
* `mvn test` - build and run all tests.
* `mvn exec:java` - start shell right away after building. You should build the project first.
* `mvn package` - build, run all tests, create JAR files in `target/` subdirectory (one without dependencies, one with full set of dependencies).
You can run the latter with `java -jar target/shell-1.0-SNAPSHOT-jar-with-dependencies.jar`.
* `mvn clean` - remove all generated files.

# Building with IntelliJ IDEA

Alternatively you should be able to seamlessly import Maven project into IDEA and run all classes/tests out of the box.
