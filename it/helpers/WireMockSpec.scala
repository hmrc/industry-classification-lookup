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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait WireMockSpec {

  val wireMockPort = 11111
  val wireMockHost = "localhost"
  val wireMockUrl = s"http://$wireMockHost:$wireMockPort"

  val wmConfig: WireMockConfiguration = wireMockConfig().port(wireMockPort)
  val wireMockServer = new WireMockServer(wmConfig)

  def startWireMock() {
    wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockPort)
  }

  def stopWireMock(): Unit = wireMockServer.stop()

  def resetWireMock(): Unit = WireMock.reset()


  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
  }

  def stubPatch(url: String, status: Integer, responseBody: String): StubMapping = {
    stubFor(patch(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
  }

  val userId = "/auth/oid/1234567890"

  def setupSimpleAuthMocks(internalId: String = "Int-xxx"): StubMapping =
    stubPost("/auth/authorise", 200,
      s"""
         |{
         |  "internalId": "$internalId",
         |  "loginTimes": {
         |     "currentLogin": "2016-11-27T09:00:00.000Z",
         |     "previousLogin": "2016-11-01T12:00:00.000Z"
         |  }
         |}""".stripMargin)
}
