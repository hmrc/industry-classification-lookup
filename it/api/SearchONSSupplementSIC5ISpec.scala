
package api

import helpers.SICSearchHelper
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import services.Indexes.ONS_SUPPLEMENT_SIC5_INDEX


class SearchONSSupplementSIC5ISpec extends SICSearchHelper {

  val indexName: String = ONS_SUPPLEMENT_SIC5_INDEX

  s"calling GET /search for $indexName index" when {

    def buildQueryAll(query: String, maxResults: Int, page: Int) = buildQuery(query, indexName, Some(maxResults), Some(page), None, queryParser = Some(true))

    "trying to search for a sic code should use the correct url" in {
      val query = "Dairy+farming"
      val client = buildQuery(query, indexName = indexName, queryParser = Some(true))
      client.url mustBe s"http://localhost:$port/industry-classification-lookup/search?query=$query&indexName=$indexName&queryParser=true"
    }

    "supplying the query 'Dairy+farming' should return a 200 and the sic code descriptions as json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 44,
        "nonFilteredFound" -> 44,
        "results" -> Json.arr(
          Json.obj("code" -> "01410", "desc" -> "Dairy farming"),
          Json.obj("code" -> "01430", "desc" -> "Stud farming"),
          Json.obj("code" -> "01450", "desc" -> "Goat farming")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Amaeth, Coedwigaeth a Physgota", "count" -> 26),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "nameCy" -> "Gweithgynhyrchu", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "nameCy" -> "Masnach Gyfanwerthu a Manwerthu; Atgyweirio Ceir a Beiciau Modur", "count" -> 8),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "nameCy" -> "Gweithgareddau mewn perthynas â Gwasanaethau Cymorth a Gweinyddol", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("Dairy+farming", indexName = indexName, Some(3), queryParser = Some(true))

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying the query 'Dairy+farming' with the sector query string 'N' should return a 200" +
      "and only the sic code descriptions in sector 'N' as well as all the sector facet counts as json" in {

      val sicCodeLookupResult = Json.obj(
        "numFound" -> 1,
        "nonFilteredFound" -> 44,
        "results" -> Json.arr(
          Json.obj("code" -> "77390", "desc" -> "Dairy machinery rental (non agricultural)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Amaeth, Coedwigaeth a Physgota", "count" -> 26),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "nameCy" -> "Gweithgynhyrchu", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "nameCy" -> "Masnach Gyfanwerthu a Manwerthu; Atgyweirio Ceir a Beiciau Modur", "count" -> 8),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "nameCy" -> "Gweithgareddau mewn perthynas â Gwasanaethau Cymorth a Gweinyddol", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("Dairy+farming", indexName = indexName, Some(5), sector = Some("N"), queryParser = Some(true))

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying a valid query and requesting page 3 should return a 200 and the sic code descriptions skipping pages 1 & 2" in {

      setupSimpleAuthMocks()

      // get results from the beginning to the end of page 3
      val pages1to3 = await(buildQueryAll("support", 15, 1).get()).json
      val p1to3docs = pages1to3.as[JsObject].value("results").as[JsArray]

      val sicCodeLookupResult = Json.obj(
        "numFound" -> 40,
        "nonFilteredFound" -> 40,
        "results" -> Json.arr(
          p1to3docs.value(10),
          p1to3docs.value(11),
          p1to3docs.value(12),
          p1to3docs.value(13),
          p1to3docs.value(14)
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "nameCy" -> "Mwyngloddio a Chwarelu", "count" -> 16),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "nameCy" -> "Gweithgynhyrchu", "count" -> 12),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "nameCy" -> "Gweithgareddau mewn perthynas â Gwasanaethau Cymorth a Gweinyddol", "count" -> 3),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "nameCy" -> "Celfyddydau, Adloniant a Hamdden", "count" -> 3),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Amaeth, Coedwigaeth a Physgota", "count" -> 2),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "nameCy" -> "Gwybodaeth a Chyfathrebu", "count" -> 2),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "nameCy" -> "Gweinyddiaeth Gyhoeddus ac Amddiffyn; Nawdd Cymdeithasol Gorfodol", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "nameCy" -> "Addysg", "count" -> 1)
        )
      )

      val client = buildQueryAll("support", 5, 3)

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying a valid query with maxResult should return a 200 and fewer sic code descriptions" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 41,
        "nonFilteredFound" -> 41,
        "results" -> Json.arr(
          Json.obj("code" -> "02100", "desc" -> "Silviculture and other forestry activities"),
          Json.obj("code" -> "32500", "desc" -> "Foot support (manufacture)"),
          Json.obj("code" -> "32500", "desc" -> "Instep support (manufacture)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "nameCy" -> "Mwyngloddio a Chwarelu", "count" -> 16),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "nameCy" -> "Gweithgynhyrchu", "count" -> 12),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Amaeth, Coedwigaeth a Physgota", "count" -> 3),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "nameCy" -> "Gweithgareddau mewn perthynas â Gwasanaethau Cymorth a Gweinyddol", "count" -> 3),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "nameCy" -> "Celfyddydau, Adloniant a Hamdden", "count" -> 3),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "nameCy" -> "Gwybodaeth a Chyfathrebu", "count" -> 2),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "nameCy" -> "Gweinyddiaeth Gyhoeddus ac Amddiffyn; Nawdd Cymdeithasol Gorfodol", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "nameCy" -> "Addysg", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("support silviculture", indexName = indexName, Some(3), queryParser = Some(true))

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying a valid query but getting no results and no facets should return the corresponding json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 0,
        "nonFilteredFound" -> 0,
        "results" -> Json.arr(),
        "sectors" -> Json.arr()
      )

      setupSimpleAuthMocks()

      val client = buildQuery("testtesttest", indexName = indexName, Some(10), queryParser = Some(true))

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying a query with no journey should default to query builder" in {


      val sicCodeLookupResult = Json.obj(
        "numFound" -> 41,
        "nonFilteredFound" -> 41,
        "results" -> Json.arr(
          Json.obj("code" -> "02100", "desc" -> "Silviculture and other forestry activities"),
          Json.obj("code" -> "32500", "desc" -> "Foot support (manufacture)"),
          Json.obj("code" -> "32500", "desc" -> "Instep support (manufacture)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "nameCy" -> "Mwyngloddio a Chwarelu", "count" -> 16),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "nameCy" -> "Gweithgynhyrchu", "count" -> 12),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Amaeth, Coedwigaeth a Physgota", "count" -> 3),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "nameCy" -> "Gweithgareddau mewn perthynas â Gwasanaethau Cymorth a Gweinyddol", "count" -> 3),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "nameCy" -> "Celfyddydau, Adloniant a Hamdden", "count" -> 3),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "nameCy" -> "Gwybodaeth a Chyfathrebu", "count" -> 2),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "nameCy" -> "Gweinyddiaeth Gyhoeddus ac Amddiffyn; Nawdd Cymdeithasol Gorfodol", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "nameCy" -> "Addysg", "count" -> 1)
        )
      )
      val client = buildQuery("support silviculture", indexName = indexName, Some(3), queryParser = None, queryBoostFirstTerm = None)

      setupSimpleAuthMocks()

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult

    }

    "return a valid set of results when using Query booster" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 41,
        "nonFilteredFound" -> 41,
        "results" -> Json.arr(
          Json.obj("code" -> "32500", "desc" -> "Foot support (manufacture)"),
          Json.obj("code" -> "32500", "desc" -> "Instep support (manufacture)"),
          Json.obj("code" -> "85600", "desc" -> "Educational support activities")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "nameCy" -> "Mwyngloddio a Chwarelu", "count" -> 16),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "nameCy" -> "Gweithgynhyrchu", "count" -> 12),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Amaeth, Coedwigaeth a Physgota", "count" -> 3),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "nameCy" -> "Gweithgareddau mewn perthynas â Gwasanaethau Cymorth a Gweinyddol", "count" -> 3),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "nameCy" -> "Celfyddydau, Adloniant a Hamdden", "count" -> 3),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "nameCy" -> "Gwybodaeth a Chyfathrebu", "count" -> 2),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "nameCy" -> "Gweinyddiaeth Gyhoeddus ac Amddiffyn; Nawdd Cymdeithasol Gorfodol", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "nameCy" -> "Addysg", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("support Silviculture", indexName = indexName, Some(3), queryParser = None, queryBoostFirstTerm = Some(true))

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying a valid query but getting no results and no facets should return the corresponding json (QUERY BOOSTER)" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 0,
        "nonFilteredFound" -> 0,
        "results" -> Json.arr(),
        "sectors" -> Json.arr()
      )

      setupSimpleAuthMocks()

      val client = buildQuery("testtesttest", indexName = indexName, Some(10), queryParser = Some(true))

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying a valid query with maxResult should return a 200 and fewer sic code descriptions (FUZZY QUERY)" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 1,
        "nonFilteredFound" -> 1,
        "results" -> Json.arr(
          Json.obj("code" -> "02100", "desc" -> "Silviculture and other forestry activities")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Amaeth, Coedwigaeth a Physgota", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("SiviculturE", indexName = indexName, Some(3), queryParser = None, queryBoostFirstTerm = None)

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }

    "supplying a valid query but getting no results and no facets should return the corresponding json (FUZZY QUERY)" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 0,
        "nonFilteredFound" -> 0,
        "results" -> Json.arr(),
        "sectors" -> Json.arr()
      )

      setupSimpleAuthMocks()

      val client = buildQuery("testtesttest", indexName = indexName, Some(10), queryParser = None, queryBoostFirstTerm = None)

      val response: WSResponse = await(client.get())

      response.status mustBe 200
      response.json mustBe sicCodeLookupResult
    }
  }

}
