import java.nio.file.Files

name := "trading-api"

organization := "ru.osfb"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

val slickPgVersion = "0.12.0"

libraryDependencies ++= Seq(
  "ru.osfb.webapi" %% "api-framework" % "0.1-SNAPSHOT",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "com.github.tminglei" %% "slick-pg" % slickPgVersion,
  //"com.github.tminglei" %% "slick-pg_jts" % slickPgVersion,
  "com.github.tminglei" %% "slick-pg_date2" % slickPgVersion
)

lazy val `collect-libs` = taskKey[Unit]("Collect library dependencies")

`collect-libs` := {
  val libraryJarPath = ((crossTarget in Compile).value / "lib").toPath
  Files.createDirectory(libraryJarPath)
  for (fileAttr <- (dependencyClasspath in Compile).value) {
    val file = fileAttr.data
    if (!file.isDirectory) Files.copy(file.toPath, libraryJarPath.resolve(file.getName))
  }
}