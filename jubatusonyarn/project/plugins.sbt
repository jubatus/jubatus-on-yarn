resolvers += "sbt-assembly-resolver-0" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"

resolvers += Classpaths.sbtPluginReleases

//addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.10.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")