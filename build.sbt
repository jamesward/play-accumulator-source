name := "play-accumulator-source"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.6.15",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"
)
