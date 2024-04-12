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
import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings

val appName: String = "industry-classification-lookup"

lazy val playSettings: Seq[Setting[_]] = LuceneIndexCreator.indexSettings

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;view.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)


ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 0

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin): _*)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(scoverageSettings : _*)
  .settings(publishingSettings: _*)
  .settings(PlayKeys.playDefaultPort := 9875)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies                           ++= AppDependencies(),
    retrieveManaged                               := true,
    update / evictionWarningOptions               := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers                                     += Resolver.jcenterRepo
  )

lazy val it = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    libraryDependencies ++= AppDependencies(),
    addTestReportOption(IntegrationTest, "int-test-reports"))