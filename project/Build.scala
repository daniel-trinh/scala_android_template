import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import android.Keys._

object ApplicationBuild extends Build with android.AutoBuild {

  import Dependencies._
  import Resolvers._

  val appName         = "HelloAndroid"
  val appVersion      = "0.1.0"

  val appDependencies = deps ++ testDeps
    Seq(
      "org.scala-lang" % "scala-compiler" % V.Scala
    )

  val appResolvers = Seq(
    jboss,
    scalaTools,
    typesafe,
    spray,
    sprayNightly,
    sonatype
  )
  val scalacSettings = Seq(
    "-Dscalac.patmat.analysisBudget=off",
    "-feature"
    // Uncomment this line to help find initialization order problems
    // "-Xcheckinit"
  )

  val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(AlignArguments, true)
      .setPreference(CompactStringConcatenation, true)
      .setPreference(SpacesAroundMultiImports, true)
      .setPreference(CompactStringConcatenation, true)
      .setPreference(CompactControlReadability, false)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(DoubleIndentClassDeclaration, false)
  }

  lazy val standardSettings = Defaults.defaultSettings ++
  SbtScalariform.scalariformSettings ++
  android.Plugin.androidBuild ++
  Seq(
    version                     := appVersion,
    scalaVersion                := V.Scala,
    organization                := "com.danieltrinh",
    ScalariformKeys.preferences := formattingPreferences,
    libraryDependencies         ++= appDependencies,
    resolvers                   ++= appResolvers,
    initialCommands             := PreRun.everything,
    proguardCache in Android ++= Seq(
      ProguardCache("org.scaloid") % "org.scaloid",
      ProguardCache("scala") % "org.scala-lang"
    ),
    dexMaxHeap in Android := "4g",
    proguardOptions in Android ++= Seq(
      "-dontobfuscate",
      "-dontoptimize",
      "-dontwarn scala.collection.mutable.**",
      "-dontwarn okio.**",
      "-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry",
      "-dontwarn javax.xml.bind.DatatypeConverter",
      "-keep class play.api.libs.functional.syntax.package$ { *; }",
      "-keep class scala.collection.SeqLike { public protected *; }",
      "-keep class org.scaloid.common.TraitContext$class { *; }",
      "-keep class macroid.Contexts$class { *; }",
      "-keep class scala.Dynamic",
      "-keep class macroid.contrib.LpTweaks$ { *; }",
      "-keep class macroid.contrib.TextTweaks$ { *; }",
      "-keep class macroid.FullDsl$ { *; }",
      "-keep class scala.Predef$ { *; }"
    ),
    run <<= run in Android,
    install <<= install in Android,
    apkbuildExcludes in Android ++= Seq(
      "META-INF/LICENSE",
      "META-INF/NOTICE",
      "META-INF/LICENSE.txt",
      "META-INF/NOTICE.txt"
    ),
    javaOptions in Test         ++= Seq(
      // Uncomment this line when there's problems loading .conf files
      // "-Dconfig.trace=loads",
      "-Dconfig.resource=test.conf"),
    scalacOptions               ++= scalacSettings
  )

  lazy val helloAndroid: Project = Project(
    id        = appName,
    base      = file("."),
    settings  = standardSettings
  )
}

object Resolvers {
  val jboss      = "JBoss repository" at
    "https://repository.jboss.org/nexus/content/repositories/"
  val scalaTools = "Scala-Tools Maven2 Snapshots Repository" at
    "http://scala-tools.org/repo-snapshots"
  val typesafe   = "Typesafe Repository" at
    "http://repo.typesafe.com/typesafe/releases/"
  val spray      = "spray repo" at
    "http://repo.spray.io"
  val sprayNightly = "spray nightly" at
    "http://nightlies.spray.io"
  val sonatype   = "Sonatype OSS" at
    "https://oss.sonatype.org"
  val jcenter    = "jcenter" at
    "http://jcenter.bintray.com"

}

object Dependencies {
  object V {
    val Scala = "2.11.1"
  }

  // Misc
  val deps = Seq(
    "org.scaloid"            %% "scaloid"   % "3.4-10",
    "com.typesafe"           %  "config"    % "1.0.2",
    "com.nineoldandroids"    %  "library"   % "2.4.0",
    "com.squareup.okhttp"    % "okhttp"     % "2.0.0",
    "com.typesafe.play"      %% "play-json" % "2.3.4",
//    "com.android.support"    % "support-v4" % "20.0.0",
//    android.Dependencies.apklib("com.viewpagerindicator" %  "library" % "2.4.1"),
    android.Dependencies.aar("org.macroid" %% "macroid" % "2.0.0-M3")
//    android.Dependencies.aar("com.daimajia.easing"    % "library"    % "1.0.0"),
//    android.Dependencies.aar("com.daimajia.swipelayout" % "library" % "1.1.7"),
//    android.Dependencies.aar("com.daimajia.androidanimations" % "library" % "1.1.2")
  )

  // Testing dependencies
  val testDeps = Seq(
    "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"
  )
}

/**
 * Commands to run before a REPL session is loaded.
 */
object PreRun {

  val everything = Imports.imports + Commands.commands

  object Imports {
    val scala =
      """
        |import scala.concurrent.{ Promise, Future }
        |import scala.concurrent.duration._
        |import scala.concurrent.Await
        |import scala.util.{ Try, Success, Failure }
        |import scala.{ Some, None }
        |import org.scaloid.common._
        |import android.graphics.Color
      """.stripMargin

    val imports = scala
  }

  object Commands {
    val commands = ""
  }

}