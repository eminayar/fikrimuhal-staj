import com.typesafe.sbt.packager.docker._

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.1"
libraryDependencies += "io.circe" %% "circe-core" % "0.9.3"
libraryDependencies += "io.circe" %% "circe-generic" % "0.9.3"
libraryDependencies += "io.circe" %% "circe-parser" % "0.9.3"
libraryDependencies += "com.pauldijou" %% "jwt-circe-legacy" % "0.16.0"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.13"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.13"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-metrics" % "2.5.13"
libraryDependencies += "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "0.14.0"
libraryDependencies += "com.lightbend.akka.discovery" %% "akka-discovery-dns" % "0.14.0"
libraryDependencies += "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "0.14.0"
libraryDependencies += "com.lightbend.akka.management" %% "akka-management" % "0.14.0"

dockerEntrypoint ++= Seq(
  """-Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")"""",
  """-Dakka.management.http.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")""""
)

dockerCommands :=
  dockerCommands.value.flatMap {
    case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
    case v => Seq(v)
  }
version := "1.3.3.7"
dockerUsername := Some("eminayar")
dockerCommands += Cmd("USER", "root")