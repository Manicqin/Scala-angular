
name := "TestWithSBT"

version := "1.0"

version := "0.0.1"

libraryDependencies ++= Seq(
  //"org.scalatest" %% "2.2.4" % "test",
  "com.twitter" %% "finatra" % "1.6.0",
  "org.squeryl" %% "squeryl" % "0.9.5-6",
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  "io.argonaut" %% "argonaut" % "6.0.4",
  "org.scalaz" %% "scalaz-core" % "7.0.0",
  "com.typesafe" % "config" % "0.4.0"
)


resolvers +=
  "Twitter" at "http://maven.twttr.com"
