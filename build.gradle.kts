plugins {
  application
  kotlin("jvm") version "1.3.71"
}

application {
  mainClassName = "app.ChatKt"
}

dependencies {
  compile(kotlin("stdlib"))
  compile("org.slf4j:slf4j-simple:1.7.25")
  compile("io.javalin:javalin:3.8.0")
  compile("org.json:json:20160810")
  compile("com.j2html:j2html:1.3.0")
}

repositories {
  jcenter()
}
