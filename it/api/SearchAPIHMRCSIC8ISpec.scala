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
import services.Indexes.HMRC_SIC8_INDEX
import services.QueryType.QUERY_PARSER

class SearchAPIHMRCSIC8ISpec extends IntegrationSpecBase {

  "calling GET /search for hmrc-sic8-codes index" when {

    val query = "Dairy+farming"

    def buildQuery(query: String, maxResults: Option[Int] = None, page: Option[Int] = None, sector: Option[String] = None, queryType: Option[String]) = {
      val maxParam = maxResults.fold("")(n => s"&pageResults=${n}")
      val indexName = s"&indexName=${HMRC_SIC8_INDEX}"
      val pageParam = page.fold("")(n => s"&page=${n}")
      val sectorParam = sector.fold("")(s => s"&sector=$s")
      val queryTypeParam = queryType.fold("")(s => s"&queryType=$s")
      buildClient(s"/search?query=${query}${indexName}${maxParam}${pageParam}${sectorParam}${queryTypeParam}")
    }

    def buildQueryAll(query: String, maxResults: Int, page: Int) = buildQuery(query, Some(maxResults), Some(page), None, Some(QUERY_PARSER))

    "trying to search for a sic code should use the correct url" in {
      val client = buildQuery(query,queryType=Some(QUERY_PARSER))
      client.url shouldBe s"http://localhost:$port/industry-classification-lookup/search?query=${query}&indexName=${HMRC_SIC8_INDEX}&queryType=${QUERY_PARSER}"
    }

    "supplying the query 'Dairy+farming' should return a 200 and the sic code descriptions as json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 36,
        "nonFilteredFound" -> 36,
        "results" -> Json.arr(
          Json.obj("code" -> "01410003", "desc" -> "Dairy farming"),
          Json.obj("code" -> "01420003", "desc" -> "Cattle farming"),
          Json.obj("code" -> "03220009", "desc" -> "Frog farming"),
          Json.obj("code" -> "01490008", "desc" -> "Fur farming"),
          Json.obj("code" -> "01490026", "desc" -> "Snail farming")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 19),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 7),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery(query, Some(5),queryType=Some(QUERY_PARSER))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying the query 'Dairy+farming' with the sector query string 'N' should return a 200" +
      "and only the sic code descriptions in sector 'N' as well as all the sector facet counts as json" in {

      val sicCodeLookupResult = Json.obj(
        "numFound" -> 1,
        "nonFilteredFound" -> 36,
        "results" -> Json.arr(
          Json.obj("code" -> "77390015", "desc" -> "Dairy machinery rental (non agricultural)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 19),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 7),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery(query, Some(5), sector = Some("N"),queryType=Some(QUERY_PARSER))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query and requesting page 3 should return a 200 and the sic code descriptions skipping pages 1 & 2" in {

      setupSimpleAuthMocks()

      // get results from the beginning to the end of page 3
      val pages1to3 = buildQueryAll(query, 15, 1).get().json
      val p1to3docs = pages1to3.as[JsObject].value("results").as[JsArray]

      val sicCodeLookupResult = Json.obj(
        "numFound" -> 36,
        "nonFilteredFound" -> 36,
        "results" -> Json.arr(
          p1to3docs.value(10),
          p1to3docs.value(11),
          p1to3docs.value(12),
          p1to3docs.value(13),
          p1to3docs.value(14)
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 19),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 7),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 1)
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
        "nonFilteredFound" -> 36,
        "results" -> Json.arr(
          Json.obj("code" -> "01410003", "desc" -> "Dairy farming"),
          Json.obj("code" -> "01420003", "desc" -> "Cattle farming"),
          Json.obj("code" -> "03220009", "desc" -> "Frog farming")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 19),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 7),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery(query, Some(3),queryType=Some(QUERY_PARSER))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query but getting no results and no facets should return the corresponding json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 0,
        "nonFilteredFound" -> 0,
        "results" -> Json.arr(),
        "sectors" -> Json.arr()
      )

      setupSimpleAuthMocks()

      val client = buildQuery("testtesttest", Some(10),queryType=Some(QUERY_PARSER))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a query with no journey should error" in {

      val client = buildQuery("testtesttest", Some(10),queryType=Some("RubbishJourney"))

      setupSimpleAuthMocks()

      val response: WSResponse = client.get()

      response.status shouldBe 500

    }
  }
}
