import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import scala.Some

name := "JubatusOnYARN"

val ORGNIZATION = "us.jubat"
val SCALA_VERSION = "2.10.4"
val VERSION = "1.0"

val JUBATUS_DEPENDENCIES = Seq(
  ("us.jubat" % "jubatus" % "0.6.0").exclude("org.jboss.netty", "netty"),
  "org.apache.hadoop" % "hadoop-common" % "2.3.0-cdh5.1.3" % "provided",
  "org.apache.hadoop" % "hadoop-hdfs" % "2.3.0-cdh5.1.3" % "provided",
  "org.apache.hadoop" % "hadoop-yarn-client" % "2.3.0-cdh5.1.3" % "provided"
)

val REST_DEPENDENCIES = Seq(
  "org.json4s" %% "json4s-native" % "3.2.10",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
)

val SERVLET_DEPENDENCIES = Seq(
  "org.scalatra" %% "scalatra" % "2.0.5",
  "org.scalatra" %% "scalatra-scalate" % "2.0.5",
  "org.scalatra" %% "scalatra-scalatest" % "2.0.5" % "test",
  "org.eclipse.jetty" % "jetty-webapp" % "7.6.0.v20120127",
  "javax.servlet" % "servlet-api" % "2.5"
)

val COMMON_DEPENDENCIES = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.slf4j" % "slf4j-log4j12" % "1.6.1",
  "log4j" % "log4j" % "1.2.16",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

val COMMON_SETTINGS = Defaults.defaultSettings ++ Seq(
  organization := ORGNIZATION,
  version := VERSION,
  scalaVersion := SCALA_VERSION,
  publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
  resolvers := Seq(
    "Jubatus Repository for Maven" at "http://download.jubat.us/maven",
    "Hadoop CDH" at "https://repository.cloudera.com/artifactory/public",
    "msgpack" at "http://msgpack.org/maven2/",
    "jubatus" at "http://download.jubat.us/maven",
    "cloudera" at "https://repository.cloudera.com/artifactory/public"
),
  //  externalResolvers <<= resolvers map { rs =>
  //    Resolver.withDefaultResolvers(rs, mavenCentral = false)
  //  },
  //  scalacOptions in(Compile, doc) ++= Seq("-groups", "-implicits", "-diagrams"),
  //  scalacOptions ++= Seq("-encoding", "UTF-8", "-feature", "-deprecation", "-Xlint"),
  //  javacOptions ++= Seq("-encoding", "UTF-8"),
  parallelExecution in Global := false
) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

val NO_PUBLISH_SETTINGS = Seq(
  publishArtifact := false,  // don't generate POMs and JARs
  publishLocal := {}  // don't generate ivy.xml either
)

lazy val root = Project(
  id = "jubatus-on-yarn",
  base = file("."),
  settings = COMMON_SETTINGS ++ NO_PUBLISH_SETTINGS
) aggregate(
  container,
  applicationMaster,
  client,
  common
  )

lazy val common = {
  val tId = "jubatus-on-yarn-common"
  Project(
    id = tId,
    base = file(tId),
    settings = COMMON_SETTINGS ++ assemblySettings ++ Seq(
      name := tId,
      libraryDependencies
        ++= COMMON_DEPENDENCIES
        ++ REST_DEPENDENCIES
        ++ SERVLET_DEPENDENCIES
        ++ JUBATUS_DEPENDENCIES
    )
  )
}

lazy val container = {
  val tId = "jubatus-on-yarn-container"
  Project(
    id = tId,
    base = file(tId),
    settings = COMMON_SETTINGS ++ NO_PUBLISH_SETTINGS ++ assemblySettings ++ Seq(
      name := tId,
      libraryDependencies
        ++= COMMON_DEPENDENCIES
        ++ REST_DEPENDENCIES
        ++ JUBATUS_DEPENDENCIES
        ++ Seq("args4j" % "args4j" % "2.0.29"),
      mainClass := Some("us.jubat.yarn.container.ContainerApp"),
      jarName := tId + VERSION + ".jar"
    )
  ) dependsOn common
}

lazy val client = {
  val tId = "jubatus-on-yarn-client"
  Project(
    id = tId,
    base = file(tId),
    settings = COMMON_SETTINGS ++ assemblySettings ++ Seq(
      name := tId,
      libraryDependencies
        ++= COMMON_DEPENDENCIES
        ++ REST_DEPENDENCIES
        ++ JUBATUS_DEPENDENCIES
//        ++ Seq("args4j" % "args4j" % "2.0.29")
    )
  ) dependsOn common
}

lazy val applicationMaster = {
  val tId = "jubatus-on-yarn-application-master"
  Project(
    id = tId,
    base = file(tId),
    settings = COMMON_SETTINGS ++ NO_PUBLISH_SETTINGS ++ assemblySettings ++ Seq(
      name := tId,
      libraryDependencies
        ++= COMMON_DEPENDENCIES
        ++ REST_DEPENDENCIES
        ++ JUBATUS_DEPENDENCIES
        ++ SERVLET_DEPENDENCIES
        ++ Seq("args4j" % "args4j" % "2.0.29"),
      mainClass := Some("us.jubat.yarn.applicationMaster.ApplicationMasterApp"),
      jarName := tId + VERSION + ".jar"
    )
  ) dependsOn common
}

lazy val test = {
  val tId = "jubatus-on-yarn-test"
  Project(
    id = tId,
    base = file(tId),
    settings = COMMON_SETTINGS ++ NO_PUBLISH_SETTINGS ++ assemblySettings ++ Seq(
      name := tId,
      libraryDependencies
        ++= COMMON_DEPENDENCIES
        ++ REST_DEPENDENCIES
        ++ JUBATUS_DEPENDENCIES,
      jarName := tId + VERSION + ".jar"
    )
  ) dependsOn(common, client)
}
