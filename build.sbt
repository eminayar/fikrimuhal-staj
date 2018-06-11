
name := "fikrimuhal-staj"
version := "0.1"
scalaVersion := "2.12.6"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-http"   % "10.1.1",
    "com.typesafe.akka" %% "akka-stream" % "2.5.11"
  )
}

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-circe-legacy" % "0.16.0"
)

libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.13"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"


val client = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "client",
    scalaVersion := "2.12.2",
    scalaJSUseMainModuleInitializer := true
  )