name := "fm-flatfile"

description := "TSV/CSV/FlatFile Reader"

scalaVersion := "3.2.2"

crossScalaVersions := Seq("3.2.2", "2.13.10", "2.12.17", "2.11.12")

val fatalWarnings = Seq(
  // Enable -Xlint, but disable the default 'unused' so we can manually specify below
  "-Xlint:-unused",
  // Remove "params" since we often have method signatures that intentionally have the parameters, but may not be used in every implementation, also omit "patvars" since it isn't part of the default xlint:unused and isn't super helpful
  "-Ywarn-unused:imports,privates,locals",
  // Warnings become Errors
  "-Xfatal-warnings"
)

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-language:implicitConversions",
  "-feature",
  "-Xlint",
) ++ (if (scalaVersion.value.startsWith("2.11")) Seq(
  // Scala 2.11 specific compiler flags
  "-Ywarn-unused-import"
) else Nil) ++ (if (scalaVersion.value.startsWith("2.12") || scalaVersion.value.startsWith("2.13")) Seq(
  // Scala 2.12/2.13 specific compiler flags
  "-opt:l:inline",
  "-opt-inline-from:<sources>"
) ++ fatalWarnings else Nil) ++ (if (scalaVersion.value.startsWith("3")) Seq(
  //"-Yno-decode-stacktraces"
) else Nil)

libraryDependencies ++= Seq(
  "com.frugalmechanic" %% "fm-common" % "1.0.1",
  "com.frugalmechanic" %% "fm-lazyseq" % "1.0.0",
  "com.frugalmechanic" %% "fm-xml" % "1.0.0",
  "com.frugalmechanic" %% "scala-optparse" % "1.2.1"
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

val ApachePOIVersion = "5.2.3"

libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % ApachePOIVersion,
  "org.apache.poi" % "poi-ooxml" % ApachePOIVersion,
  "org.apache.poi" % "poi-ooxml-lite" % ApachePOIVersion,
  "com.fasterxml.woodstox" % "woodstox-core" % "5.1.0"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test

publishTo := sonatypePublishToBundle.value
