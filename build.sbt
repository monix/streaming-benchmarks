name := "streaming-benchmarks"

organization := "io.monix"
scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "io.monix"             %% "monix"       % "3.0.0-RC2",
  "com.typesafe.akka"    %% "akka-stream" % "2.5.17",
  "co.fs2"               %% "fs2-core"    % "1.0.0",
  "org.typelevel"        %% "cats-effect" % "1.0.0",
  "io.reactivex.rxjava2" %  "rxjava"      % "2.2.3"
)

enablePlugins(JmhPlugin)
