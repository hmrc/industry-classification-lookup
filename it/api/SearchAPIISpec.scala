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
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSResponse

class SearchAPIISpec extends IntegrationSpecBase {

  "calling GET /search" when {

    val query = "Dairy+farming"

    def buildQuery(query: String, maxResults: Option[Int] = None, page: Option[Int] = None) = {
      val maxParam = maxResults.fold("")(n => s"&pageResults=${n}")
      val pageParam = page.fold("")(n => s"&page=${n}")
      buildClient(s"/search?query=${query}${maxParam}${pageParam}")
    }
    def buildQueryAll(query: String, maxResults: Int, page: Int) = buildQuery(query, Some(maxResults), Some(page))

    "trying to search for a sic code should use the correct url" in {
      val client = buildQuery(query)
      client.url shouldBe s"http://localhost:$port/industry-classification-lookup/search?query=$query"
    }

    "supplying a valid query should return a 200 and the sic code descriptions as json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 36,
        "results" -> Json.arr(
          Json.obj("code" -> "01410003", "desc" -> "Dairy farming"),
          Json.obj("code" -> "01420003", "desc" -> "Cattle farming"),
          Json.obj("code" -> "03220009", "desc" -> "Frog farming"),
          Json.obj("code" -> "01490008", "desc" -> "Fur farming"),
          Json.obj("code" -> "01490026", "desc" -> "Snail farming")
        ),
        "facets" -> Json.arr(
          Json.obj("code" -> "A", "count" -> 19),
          Json.obj("code" -> "C", "count" -> 9),
          Json.obj("code" -> "G", "count" -> 7),
          Json.obj("code" -> "N", "count" -> 1)
        )
      )

      val client = buildQuery(query, Some(5))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query and requesting page 3 should return a 200 and the sic code descriptions skipping pages 1 & 2" in {

      // get results from the beginning to the end of page 3
      val pages1to3 = buildQueryAll(query, 15, 1).get().json
      val p1to3docs = pages1to3.as[JsObject].value("results").as[JsArray]

      val sicCodeLookupResult = Json.obj(
        "numFound" -> 36,
        "results" -> Json.arr(
          p1to3docs.value(10),
          p1to3docs.value(11),
          p1to3docs.value(12),
          p1to3docs.value(13),
          p1to3docs.value(14)
        ),
        "facets" -> Json.arr(
          Json.obj("code" -> "A", "count" -> 57),
          Json.obj("code" -> "C", "count" -> 27),
          Json.obj("code" -> "G", "count" -> 21),
          Json.obj("code" -> "N", "count" -> 3)
        )
      )

      val client = buildQueryAll(query, 5, 3)

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query with maxResult should return a 200 and fewer sic code descriptions" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 36,
        "results" -> Json.arr(
          Json.obj("code" -> "01410003", "desc" -> "Dairy farming"),
          Json.obj("code" -> "01420003", "desc" -> "Cattle farming"),
          Json.obj("code" -> "03220009", "desc" -> "Frog farming")
        ),
        "facets" -> Json.arr(
          Json.obj("code" -> "A", "count" -> 76),
          Json.obj("code" -> "C", "count" -> 36),
          Json.obj("code" -> "G", "count" -> 28),
          Json.obj("code" -> "N", "count" -> 4)
        )
      )

      val client = buildQuery(query, Some(3))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query but getting no results and no facets should return the corresponding json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 0,
        "results" -> Json.arr(),
        "facets" -> Json.arr()
      )

      val client = buildQuery("testtesttest", Some(10))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }
  }
}
