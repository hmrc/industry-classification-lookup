import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object MicroServiceBuild extends Build with MicroService {

  val appName = "industry-classification-lookup"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test() ++ it()

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo" % "6.1.0",
    "org.apache.lucene" % "lucene-core" % "7.1.0",
    "org.apache.lucene" % "lucene-queryparser" % "7.1.0",
    "uk.gov.hmrc" %% "microservice-bootstrap" % "6.9.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % scope,
    "org.scalatest" %% "scalatest" % "3.0.1" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" % "mockito-core" % "1.9.5" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope
  )

  def it(scope: String = "it") = Seq(
    "com.github.tomakehurst" % "wiremock" % "2.5.0" % scope
  )
}
