name := "streaming-benchmarks"

organization := "io.monix"
scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "3.1.0",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "co.fs2" %% "fs2-core" % "2.3.0",
  "org.typelevel" %% "cats-effect" % "2.1.2",
  "io.reactivex.rxjava2" % "rxjava" % "2.2.19",
  "dev.zio" %% "zio" % "1.0.0-RC18-2",
  "dev.zio" %% "zio-streams" % "1.0.0-RC18-2",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC12"
)

enablePlugins(JmhPlugin)
