/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "industry-classification-lookup"

lazy val playSettings : Seq[Setting[_]] = LuceneIndexCreator.indexSettings

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;view.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin): _*)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(majorVersion := 0)
  .settings(scoverageSettings : _*)
  .settings(publishingSettings: _*)
  .settings(PlayKeys.playDefaultPort := 9875)
  .settings(defaultSettings(): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    scalaVersion                                  := "2.12.12",
    libraryDependencies                           ++= AppDependencies(),
    retrieveManaged                               := true,
    update / evictionWarningOptions               := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    IntegrationTest / Keys.fork                   := false,
    IntegrationTest / parallelExecution           := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    resolvers                                     += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers                                     += Resolver.jcenterRepo,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )