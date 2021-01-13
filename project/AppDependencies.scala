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

import play.core.PlayVersion
import sbt._

private object AppDependencies {
  def apply() = MainDependencies() ++ LuceneDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object LuceneDependencies {
  private val luceneVersion             = "7.1.0"

  def apply() = Seq(
    "org.apache.lucene" % "lucene-core" % luceneVersion,
    "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
    "org.apache.lucene" % "lucene-facet" % luceneVersion
  )
}

object MainDependencies {
  private val authClientVersion        = "3.2.0-play-26"
  private val bootstrapPlayVersion   = "3.2.0"
  private val playReactiveMongoVersion = "7.31.0-play-26"

  def apply() = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-26" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % playReactiveMongoVersion,
    "uk.gov.hmrc" %% "auth-client" % authClientVersion
  )
}

trait TestDependencies {
  val scalaTestPlusVersion  = "3.1.3"
  val pegdownVersion        = "1.6.0"
  val mockitoCoreVersion    = "3.3.3"
  val wiremockVersion       = "2.26.3"

  val scope: Configuration
  val test: Seq[ModuleID]

  lazy val coreTestDependencies = Seq(
    "org.scalatestplus.play"  %%  "scalatestplus-play"  % scalaTestPlusVersion  % scope,
    "org.pegdown"             %   "pegdown"             % pegdownVersion        % scope,
    "com.typesafe.play"       %%  "play-test"           % PlayVersion.current   % scope
  )
}

object UnitTestDependencies extends TestDependencies {
  override val scope = Test
  override val test  = coreTestDependencies ++ Seq(
    "org.mockito" % "mockito-core" % mockitoCoreVersion % scope
  )

  def apply() = test
}

object IntegrationTestDependencies extends TestDependencies {
  override val scope = IntegrationTest
  override val test  =  coreTestDependencies ++ Seq(
    "com.github.tomakehurst"  %  "wiremock-jre8" % wiremockVersion  % scope
  )

  def apply() = test
}
