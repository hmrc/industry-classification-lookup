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

class SearchAPIISpec extends IntegrationSpecBase {

  "calling GET /search" when {

    val query = "Dairy+farming"

    def buildQuery(query: String, maxResults: Option[Int] = None, page: Option[Int] = None) = {
      val maxParam = maxResults.fold("")(n => s"&pageResults=${n}")
      val pageParam = page.fold("")(n => s"&page=${n}")
      buildClient(s"/search?query=${query}${maxParam}${pageParam}")
    }
    def buildQueryAll(query: String, maxResults: Int, page: Int) = buildQuery(query, Some(maxResults), Some(page))

    "trying to seach for a sic code should use the correct url" in {
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
        )
      )

      val client = buildQuery(query, Some(5))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query and requesting page 3 should return a 200 and the sic code descriptions skipping pages 1 & 2" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 36,
        "results" -> Json.arr(
          Json.obj("code" -> "46610005", "desc" -> "Dairy farm machinery (wholesale)"),
          Json.obj("code" -> "46330004", "desc" -> "Dairy produce exporter (wholesale)"),
          Json.obj("code" -> "46330005", "desc" -> "Dairy produce importer (wholesale)"),
          Json.obj("code" -> "46330006", "desc" -> "Dairy produce n.e.c (wholesale)"),
          Json.obj("code" -> "47290002", "desc" -> "Dairy products (retail)")
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
        )
      )

      val client = buildQuery(query, Some(3))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }
  }
}
