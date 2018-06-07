libraryDependencies += "org.querki" %%% "jquery-facade" % "1.2"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.2"

skip in packageJSDependencies := false
jsDependencies +=
  "org.webjars" % "jquery" % "2.1.4" / "2.1.4/jquery.js"