name := "fikrimuhal-staj"
version := "0.1"
scalaVersion := "2.12.6"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.1"
libraryDependencies += "io.circe" %% "circe-core" % "0.9.3"
libraryDependencies += "io.circe" %% "circe-generic" % "0.9.3"
libraryDependencies += "io.circe" %% "circe-parser" % "0.9.3"
libraryDependencies += "com.pauldijou" %% "jwt-circe-legacy" % "0.16.0"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.13"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.13"


val client = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "client",
    scalaVersion := "2.12.2",
    scalaJSUseMainModuleInitializer := true
  )