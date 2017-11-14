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

package controllers

import helpers.ControllerSpec
import models.SicCode
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.LookupService
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}

class LookupControllerSpec extends ControllerSpec {

  val mockLookupService: LookupService = mock[LookupService]

  trait Setup {
    val controller: LookupController = new LookupController {
      val lookupService: LookupService = mockLookupService
    }
  }

  "lookup" should {

    val sicCode = "12345678"
    val sicCodeLookupResult = SicCode(sicCode, "test description")
    val sicCodeResultAsJson = Json.parse(
      s"""
         |{
         |  "code":"$sicCode",
         |  "desc":"test description"
         |}
      """.stripMargin).as[JsObject]

    "return a 200 when a sic code description is returned from LookupService" in new Setup {
      when(mockLookupService.lookup(eqTo(sicCode)))
        .thenReturn(Some(sicCodeLookupResult))

      val result: Result = controller.lookup(sicCode)(FakeRequest())
      status(result) shouldBe 200
      bodyAsJson(result) shouldBe sicCodeResultAsJson
    }

    "return a 404 when nothing is returned from LookupService" in new Setup {
      when(mockLookupService.lookup(eqTo(sicCode)))
        .thenReturn(None)

      val result: Result = controller.lookup(sicCode)(FakeRequest())
      status(result) shouldBe 404
    }
  }
}
