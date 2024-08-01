name := "akka-fsm-example"

version := "0.1"

scalaVersion := "2.13.8"  // Puedes usar la versión de Scala que prefieras

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % "1.0.2",
  "ch.qos.logback" % "logback-classic" % "1.2.13",
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % "1.0.2" ,   
)
