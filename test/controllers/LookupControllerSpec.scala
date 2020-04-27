/*
 * Copyright 2020 HM Revenue & Customs
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

import helpers.{AuthHelper, ControllerSpec}
import models.SicCode
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.LookupService
import uk.gov.hmrc.auth.core.PlayAuthConnector

import scala.concurrent.Future

class LookupControllerSpec extends ControllerSpec with AuthHelper {

  val mockLookupService: LookupService = mock[LookupService]
  override val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]

  trait Setup {
    val controller: LookupController = new LookupController(mockLookupService, mockAuthConnector, stubControllerComponents())
  }

  "lookup" should {

    val sicCode = "12345678"

    "return an Ok" when {
      "when a single sic code is supplied and a result is found" in new Setup {
        val sicCodeLookupResult = SicCode(sicCode, "test description")
        val sicCodeResultAsJson = Json.parse(
          s"""
             |[
             |  {
             |    "code":"$sicCode",
             |    "desc":"test description"
             |  }
             |]
      """.stripMargin)

        when(mockLookupService.lookup(eqTo(List(sicCode))))
          .thenReturn(List(sicCodeLookupResult))
        mockAuthorisedRequest(Future.successful({}))

        val result: Future[Result] = controller.lookup(sicCode)(FakeRequest())
        status(result) mustBe OK
        contentAsJson(result) mustBe sicCodeResultAsJson
      }

      "matching results have been found" in new Setup {
        val sicCodeResultAsJson = Json.parse(
          s"""
             |[
             |  {
             |    "code":"testCode",
             |    "desc":"test description"
             |  },
             |  {
             |    "code":"testCode2",
             |    "desc":"test description"
             |  }
             |]
      """.stripMargin)

        when(mockLookupService.lookup(any()))
          .thenReturn(List(SicCode("testCode", "test description"), SicCode("testCode2", "test description")))
        mockAuthorisedRequest(Future.successful({}))

        val result: Future[Result] = controller.lookup("testCode,testCode2")(FakeRequest())
        status(result) mustBe OK
        contentAsJson(result) mustBe sicCodeResultAsJson
      }
    }

    "return a NoContent" when {
      "the request was successful but no results were found" in new Setup {
        when(mockLookupService.lookup(any()))
          .thenReturn(List.empty[SicCode])
        mockAuthorisedRequest(Future.successful({}))

        val result: Future[Result] = controller.lookup("testCode,testCode2")(FakeRequest())
        status(result) mustBe NO_CONTENT
      }
    }
  }
}
