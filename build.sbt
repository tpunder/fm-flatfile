name := "fm-flatfile"

organization := "com.frugalmechanic"

version := "0.1-SNAPSHOT"

description := "TSV/CSV/FlatFile Reader"

licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/frugalmechanic/fm-flatfile"))

scalaVersion := "2.10.4"

// Note: Use "++ 2.11.0" to select a specific version when building
crossScalaVersions := Seq("2.10.4", "2.11.0")

scalacOptions := Seq("-unchecked", "-deprecation", "-language:implicitConversions", "-feature", "-optimise")

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.frugalmechanic" %% "fm-common" % "0.1-SNAPSHOT",
  "com.frugalmechanic" %% "fm-lazyseq" % "0.1-SNAPSHOT",
  "com.frugalmechanic" %% "scala-optparse" % "1.1.1"
)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5"
)

// These dependencies are for Excel reading support.
//
// TODO: either get Progaurd working with the Apache POI stuff or
//       break out Excel support as a separate package.  I got Proguard
//       working with the XLS support but not XLSX support due to Apache
//       XMLBeans doing weird things.
//
libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % "3.10-FINAL",
  "org.apache.poi" % "poi-ooxml" % "3.10-FINAL",
  "org.apache.poi" % "poi-ooxml-schemas" % "3.10-FINAL",
  "org.codehaus.woodstox" % "woodstox-core-asl" % "4.3.0" % "embedded"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.3" % "test"

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <developers>
    <developer>
      <id>tim</id>
      <name>Tim Underwood</name>
      <email>tim@frugalmechanic.com</email>
      <organization>Frugal Mechanic</organization>
      <organizationUrl>http://frugalmechanic.com</organizationUrl>
    </developer>
    <developer>
      <id>eric</id>
      <name>Eric Peters</name>
      <email>eric@frugalmechanic.com</email>
      <organization>Frugal Mechanic</organization>
      <organizationUrl>http://frugalmechanic.com</organizationUrl>
    </developer>
  </developers>
  <scm>
      <connection>scm:git:git@github.com:frugalmechanic/fm-flatfile.git</connection>
      <developerConnection>scm:git:git@github.com:frugalmechanic/fm-flatfile.git</developerConnection>
      <url>git@github.com:frugalmechanic/fm-flatfile.git</url>
  </scm>)

