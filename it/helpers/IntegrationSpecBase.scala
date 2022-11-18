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

package helpers

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}

trait IntegrationSpecBase extends PlaySpec with WireMockSpec with GuiceOneServerPerSuite
  with BeforeAndAfterEach with BeforeAndAfterAll {

  val wiremockPort = 11111
  val wiremockHost = "localhost"
  val testAuthToken = "testAuthToken"
  val mockHost = wiremockHost
  val mockPort = wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val extraConfig = Map(
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort"
  )

  //The application used for integration tests
  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(extraConfig)
    .build()

  private def wsClient(implicit app: Application): WSClient = app.injector.instanceOf[WSClient]

  def buildClient(path: String)(implicit app: Application): WSRequest = {
    wsClient.url(s"http://localhost:$port/industry-classification-lookup$path")
      .withHttpHeaders("authorization" -> testAuthToken)
  }

  override def beforeEach() {
    resetWireMock()
  }

  override def beforeAll() {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll() {
    stopWireMock()
    super.afterAll()
  }
}
