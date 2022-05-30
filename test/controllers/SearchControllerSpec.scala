/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ICLConfig
import helpers.{AuthHelper, ControllerSpec}
import models.SicCode
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{LookupService, SearchResult}
import uk.gov.hmrc.auth.core.PlayAuthConnector

import scala.concurrent.Future

class SearchControllerSpec extends ControllerSpec with AuthHelper {

  val mockLookupService: LookupService = mock[LookupService]
  val mockICLConfig: ICLConfig = mock[ICLConfig]
  override val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]

  trait Setup {
    val controller: SearchController = new SearchController(mockLookupService, mockICLConfig, mockAuthConnector, stubControllerComponents()) {
      override val defaultIndex: String = "bar"
    }
  }

  "search" should {

    val query = "12345678"
    val desc = "test description"
    val sicCodeLookupResult = SearchResult(1, 1, Seq(SicCode(query, desc)), Seq())
    val sicCodeResultAsJson = Json.obj(
      "numFound" -> 1,
      "nonFilteredFound" -> 1,
      "results" -> Json.arr(Json.obj(
        "code" -> query,
        "desc" -> desc
      )),
      "sectors" -> Json.arr()
    )

    "return a 200 when a sic code description is returned from LookupService" in new Setup {
      when(mockLookupService.search(eqTo(query), eqTo("foo"), eqTo(None), eqTo(None), eqTo(None), eqTo(None), eqTo(None)))
        .thenReturn(sicCodeLookupResult)
      mockAuthorisedRequest(Future.successful({}))

      val result: Future[Result] = controller.search(query, Some("foo"), None, None)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe sicCodeResultAsJson
    }

    "return a 404 when no description is returned from LookupService" in new Setup {
      when(mockLookupService.search(eqTo(query), eqTo("foo"), eqTo(None), eqTo(None), eqTo(None), eqTo(None), eqTo(None)))
        .thenReturn(SearchResult(0, 0, Seq(), Seq()))
      mockAuthorisedRequest(Future.successful({}))

      val result: Future[Result] = controller.search(query, Some("foo"), None, None)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe Json.obj(
        "numFound" -> 0,
        "nonFilteredFound" -> 0,
        "results" -> Json.arr(),
        "sectors" -> Json.arr()
      )
    }

    "Use the default index when one isn't specified" in new Setup {
      when(mockLookupService.search(eqTo(query), eqTo("bar"), eqTo(None), eqTo(None), eqTo(None), eqTo(None), eqTo(None)))
        .thenReturn(sicCodeLookupResult)
      mockAuthorisedRequest(Future.successful({}))

      val result: Future[Result] = controller.search(query, None, None, None)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe sicCodeResultAsJson
    }

  }
}
