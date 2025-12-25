name := "protocol"

organization := "com.example"

version := "0.1.0"

scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "3.25.1",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

// Use standard Maven proto directory
Compile / PB.protoSources := Seq(sourceDirectory.value / "main" / "proto")

Compile / PB.targets := Seq(
  // Generate Java classes for Maven projects
  PB.gens.java -> (Compile / sourceManaged).value / "java",
  // Generate Scala classes for SBT projects
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

// Enable JAR packaging for use as a dependency
Compile / packageBin / publishArtifact := true

// Allow overwriting when republishing
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
publishM2Configuration := publishM2Configuration.value.withOverwrite(true)

