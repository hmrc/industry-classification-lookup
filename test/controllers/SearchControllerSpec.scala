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
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.{LookupService, SearchResult}

class SearchControllerSpec extends ControllerSpec {

  val mockLookupService: LookupService = mock[LookupService]

  trait Setup {
    val controller: SearchController = new SearchController {
      val lookupService: LookupService = mockLookupService
    }
  }

  "search" should {

    val query = "12345678"
    val desc = "test description"
    val sicCodeLookupResult = SearchResult(1, Seq(SicCode(query, desc)))
    val sicCodeResultAsJson = Json.obj(
      "numFound" -> 1,
      "results" -> Json.arr(Json.obj(
        "code" -> query,
        "desc" -> desc
      ))
    )

    "return a 200 when a sic code description is returned from LookupService" in new Setup {

      when(mockLookupService.search(eqTo(query), eqTo(None), eqTo(None)))
        .thenReturn(sicCodeLookupResult)

      val result: Result = controller.search(query,  None, None)(FakeRequest())
      status(result) shouldBe 200
      bodyAsJson(result) shouldBe sicCodeResultAsJson
    }

    "return a 404 when no description is returned from LookupService" in new Setup {

      when(mockLookupService.search(eqTo(query), eqTo(None), eqTo(None)))
        .thenReturn(SearchResult(0, Seq()))

      val result: Result = controller.search(query, None, None)(FakeRequest())
      status(result) shouldBe 200
      bodyAsJson(result) shouldBe Json.obj(
        "numFound" -> 0,
        "results" -> Json.arr()
      )
    }

  }
}
