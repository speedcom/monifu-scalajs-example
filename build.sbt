import sbt.Project.projectToRef

lazy val clients = Seq(client)
lazy val scalaV = "2.11.7"

lazy val server = (project in file("server")).settings(
  scalaVersion    := scalaV,
  scalaJSProjects := clients,
  pipelineStages  := Seq(scalaJSProd, gzip),
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    "com.vmunier"                %% "play-scalajs-scripts" % "0.3.0",
    "org.webjars"                %  "jquery"               % "1.11.1",
    "org.monifu"                 %% "monifu"               % "1.0-RC4",
    "com.typesafe.scala-logging" %% "scala-logging"        % "3.1.0",
    specs2 % Test
  )
).enablePlugins(PlayScala).
  aggregate(clients.map(projectToRef): _*).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion            := scalaV,
  persistLauncher         := true,
  persistLauncher in Test := false,
  sourceMapsDirectories += sharedJs.base / "..",
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    "org.monifu"   %%% "monifu"      % "1.0-RC2"
  )
).enablePlugins(
    ScalaJSPlugin,
    ScalaJSPlay
).dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSPlay).
  jsSettings(sourceMapsBase := baseDirectory.value / "..")

lazy val sharedJvm = shared.jvm
lazy val sharedJs  = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
