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

import helpers.SICSearchHelper
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSResponse
import services.Indexes.HMRC_SIC8_INDEX
import services.QueryType._

class SearchAPIHMRCSIC8ISpec extends SICSearchHelper {

  "calling GET /search for hmrc-sic8-codes index" when {

    val query = "Dairy farming"

    def buildQueryAll(query: String, maxResults: Int, page: Int) = buildQuery(query, HMRC_SIC8_INDEX, Some(maxResults), Some(page), None, Some(true))

    "trying to search for a sic code should use the correct url" in {
      val client = buildQuery(query, indexName = HMRC_SIC8_INDEX, queryParser=Some(true))
      client.url shouldBe s"http://localhost:$port/industry-classification-lookup/search?query=$query&indexName=$HMRC_SIC8_INDEX&queryParser=true"
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

      val client = buildQuery(query, indexName = HMRC_SIC8_INDEX, Some(5),queryParser=Some(true))

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

      val client = buildQuery(query, indexName = HMRC_SIC8_INDEX, Some(5), sector = Some("N"),queryParser=Some(true))

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

      val client = buildQuery(query, indexName = HMRC_SIC8_INDEX, Some(3), queryParser=Some(true))

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

      val client = buildQuery("testtesttest", indexName = HMRC_SIC8_INDEX, Some(10), queryParser = Some(true))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a query with no queryParser parameter and no queryBoostFirstTerm parameter should perform a default query builder" in {
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
      val client = buildQuery(query, indexName = HMRC_SIC8_INDEX, Some(5), queryParser = None, queryBoostFirstTerm = None)

      setupSimpleAuthMocks()

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "return a valid set of results when using Query booster" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 36,
        "nonFilteredFound" -> 36,
        "results" -> Json.arr(
          Json.obj("code" -> "03220009", "desc" -> "Frog farming"),
          Json.obj("code" -> "01410003", "desc" -> "Dairy farming"),
          Json.obj("code" -> "01420003", "desc" -> "Cattle farming")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 19),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 7),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("frog dAirY farMing", indexName = HMRC_SIC8_INDEX, Some(3), queryBoostFirstTerm = Some(true))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json   shouldBe sicCodeLookupResult
    }

    "supplying a valid query but getting no results and no facets should return the corresponding json (QUERY BOOSTER)" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 0,
        "nonFilteredFound" -> 0,
        "results" -> Json.arr(),
        "sectors" -> Json.arr()
      )

      setupSimpleAuthMocks()

      val client = buildQuery("testtesttest", indexName = HMRC_SIC8_INDEX, Some(10), queryBoostFirstTerm = Some(true))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }
  }
}
