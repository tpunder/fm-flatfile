FMPublic

name := "fm-flatfile"

version := "0.2.0"

description := "TSV/CSV/FlatFile Reader"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

scalacOptions := Seq("-unchecked", "-deprecation", "-language:implicitConversions", "-feature", "-Xlint", "-optimise", "-Yinline-warnings")

libraryDependencies ++= Seq(
  "com.frugalmechanic" %% "fm-common" % "0.2.0",
  "com.frugalmechanic" %% "fm-lazyseq" % "0.2.0",
  "com.frugalmechanic" %% "fm-xml" % "0.1.0",
  "com.frugalmechanic" %% "scala-optparse" % "1.1.1"
)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5"
)

// These dependencies are for Excel reading support.
//
// Proguard support is kind of working.  I'm able to strip out
// most of the un-used stuff but the package name space is still
// polluted due to Apache xmlbeans doing weird things with reflection
// and .xsb files.  Still might have to just break excel support out
// into a separate package at some point for people who don't want
// the extra dependencies or have package/class conflicts.
//
// The other option I can think of is to do some class loader trickery
// where at package time we somehow move all the dependencies into a 
// subdirectory (e.g. fm/flatfile/thirdparty/org/xmlbeans/...) and then use
// a custom class loader that automatically prepends the fm/flatfile/thirdparty
// part when loading resources or classes for the Excel support.
//
// Update - Getting some runtime errors when using Proguard so I'm
//          disabling it for now until I can come up with a better
//          solution.
//
libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % "3.10-FINAL",
  "org.apache.poi" % "poi-ooxml" % "3.10-FINAL",
  "org.apache.poi" % "poi-ooxml-schemas" % "3.10-FINAL",
  "org.codehaus.woodstox" % "woodstox-core-asl" % "4.3.0"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.3" % "test"
