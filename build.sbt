name := "play-accumulator-source"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.6.0-RC1",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0-RC1" % "test"
)
