FMPublic

name := "fm-flatfile"

description := "TSV/CSV/FlatFile Reader"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.11.11", "2.12.8")

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-language:implicitConversions",
  "-feature",
  "-Xlint",
  "-Ywarn-unused-import"
) ++ (if (scalaVersion.value.startsWith("2.12")) Seq(
  // Scala 2.12 specific compiler flags
  "-opt:l:inline",
  "-opt-inline-from:<sources>"
) else Nil)

libraryDependencies ++= Seq(
  "com.frugalmechanic" %% "fm-common" % "0.32.0",
  "com.frugalmechanic" %% "fm-lazyseq" % "0.11.0",
  "com.frugalmechanic" %% "fm-xml" % "0.14.0",
  "com.frugalmechanic" %% "scala-optparse" % "1.1.2"
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

val ApachePOIVersion = "4.0.1"

libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % ApachePOIVersion,
  "org.apache.poi" % "poi-ooxml" % ApachePOIVersion,
  "org.apache.poi" % "poi-ooxml-schemas" % ApachePOIVersion,
  "com.fasterxml.woodstox" % "woodstox-core" % "5.1.0"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
