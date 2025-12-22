name := "protocol"

version := "0.1.0"

scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "3.25.1",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
