organization := "com.whisk"

name := "docker-it-scala"

version := "0.1"

scalaVersion := "2.11.6"

scalariformSettings

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

fork in Test := true

libraryDependencies ++= Seq(
  "com.github.docker-java" % "docker-java" % "0.10.4",
  "me.lessis" %% "odelay-core" % "0.1.0",
  "me.lessis" %% "undelay" % "0.1.0",
  "org.scalatest" %% "scalatest" % "2.2.4",
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "test")
