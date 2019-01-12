name := "streaming-benchmarks"

organization := "io.monix"
scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "io.monix"             %% "monix"       % "3.0.0-RC2",
  "com.typesafe.akka"    %% "akka-stream" % "2.5.19",
  "co.fs2"               %% "fs2-core"    % "1.0.2",
  "org.typelevel"        %% "cats-effect" % "1.1.0",
  "io.reactivex.rxjava2" %  "rxjava"      % "2.2.5"
)

enablePlugins(JmhPlugin)
