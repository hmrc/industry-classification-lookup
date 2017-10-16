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

package api

import helpers.IntegrationSpecBase
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

class LookupAPIISpec extends IntegrationSpecBase {

  "calling GET /lookup" when {

    val sicCodeDescription = Json.parse(
      """
        |{
        |  "currentFRSPercent":"6.5",
        |  "description":"Barley, oats or wheat growing",
        |  "frsCategory":"Farming or agriculture that is not listed elsewhere",
        |  "displayDetails":"Growing of cereals (except rice), leguminous crops and oil seeds"
        |}
      """.stripMargin)

    "trying to lookup a sic code should use the correct url" in {
      val sicCode = "01110001"
      val client = buildClient(s"/lookup/$sicCode")
      client.url shouldBe s"http://localhost:$port/industry-classification-lookup/lookup/$sicCode"
    }

    "supplying a valid sic code should return a 200 and the sic code description as json" in {
      val sicCode = "01110001"
      val client = buildClient(s"/lookup/$sicCode")

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeDescription
    }

    "supplying an invalid sic code should return a 404" in {
      val invalidSicCode = "abc123"
      val client = buildClient(s"/lookup/$invalidSicCode")

      val response: WSResponse = client.get()

      response.status shouldBe 404
    }
  }
}
