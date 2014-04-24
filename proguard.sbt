import com.typesafe.sbt.SbtProguard._
import com.typesafe.sbt.SbtProguard.ProguardKeys.proguard

// Modeled after https://github.com/rtimush/sbt-updates/blob/master/proguard.sbt

proguardSettings

javaOptions in (Proguard, proguard) := Seq("-Xmx1024M")

ProguardKeys.proguardVersion in Proguard := "4.11"

ProguardKeys.options in Proguard ++= Seq(
  "-dontoptimize",
  "-dontusemixedcaseclassnames", // Don't write out i.class and I.class (which won't unjar properly on case-insensitive file systems like on OSX)
  "-keep public class fm.** { *; }",
  "-keepclassmembers class ** extends org.apache.poi.hssf.record.Record { public final static short sid; }",
  "-keepclassmembers class ** extends org.apache.poi.hssf.record.Record { public static *** create(...); }",
  "-keepclassmembers class ** extends org.apache.poi.hssf.record.Record { public <init>(...); }",
  "-keepclassmembers class org.apache.xmlbeans.** { <fields>; <methods>; }",
  "-keep class org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl",
  "-keep class schemaorg_apache_xmlbeans.** { *; }",
  "-adaptclassstrings org.apache.xmlbeans.**",
  "-repackageclasses fm.flatfile.libs",
  "-keepattributes",
  "-keepparameternames",
  //"-dontnote org.joda.convert.StringConvert,org.joda.convert.AnnotationStringConverterFactory,org.joda.time.DateTimeZone",
  "-dontnote org.apache.**,org.dom4j.**",
  "-dontwarn org.apache.**,org.dom4j.**,com.ctc.**",
  "-dontwarn com.microsoft.**,schemasMicrosoftCom**,org.openxmlformats.**"
)

//ProguardKeys.defaultInputFilter in Proguard := Some("!META-INF/**,!schemaorg_apache_xmlbeans/**,!javax/**,!org/xml/sax/**,!org/w3c/dom/**,!font_metrics.properties,!**.txt,!**.template,!**.properties,!**.pptx,!**.xml")

ProguardKeys.defaultInputFilter in Proguard := Some("!META-INF/**,!javax/**,!org/xml/sax/**,!org/w3c/dom/**,!license/**,!LICENSE.txt,!NOTICE.txt")

// Some of the Apache libs need javax.crypto
//ProguardKeys.libraries in Proguard += new File(System.getProperty("java.home"), "lib/jce.jar")

ProguardKeys.inputs in Proguard <<= (dependencyClasspath in Embedded, packageBin in Runtime) map {
  (dcp, pb) => Seq(pb) ++ dcp.files
}

Build.publishMinJar <<= (ProguardKeys.proguard in Proguard) map (_.head)

packagedArtifact in (Compile, packageBin) <<= (packagedArtifact in (Compile, packageBin), Build.publishMinJar) map {
  case ((art, _), jar) => (art, jar)
}

dependencyClasspath in Compile <++= dependencyClasspath in Embedded

dependencyClasspath in Test <++= dependencyClasspath in Embedded