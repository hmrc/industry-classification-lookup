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
  private val luceneVersion = "7.1.0"

  def apply() = Seq(
    "org.apache.lucene" % "lucene-core" % luceneVersion,
    "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
    "org.apache.lucene" % "lucene-facet" % luceneVersion
  )
}

object MainDependencies {
  private val bootstrapPlayVersion = "5.16.0"
  private val playReactiveMongoVersion = "8.0.0-play-28"

  def apply() = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % playReactiveMongoVersion,
  )
}

trait TestDependencies {
  val scalaTestPlusVersion = "5.1.0"
  val mockitoCoreVersion = "4.1.0"
  val wiremockVersion = "2.31.0"

  val scope: Configuration
  val test: Seq[ModuleID]

  lazy val coreTestDependencies = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
  )
}

object UnitTestDependencies extends TestDependencies {
  override val scope = Test
  override val test = coreTestDependencies ++ Seq(
    "org.mockito" % "mockito-core" % mockitoCoreVersion % scope,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % scope
  )

  def apply() = test
}

object IntegrationTestDependencies extends TestDependencies {
  override val scope = IntegrationTest
  override val test = coreTestDependencies ++ Seq(
    "com.github.tomakehurst" % "wiremock-jre8-standalone" % wiremockVersion % scope
  )

  def apply() = test
}
