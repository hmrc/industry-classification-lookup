
package api

import helpers.SICSearchHelper
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSResponse
import services.Indexes.ONS_SUPPLEMENT_SIC5_INDEX
import services.QueryType._


class SearchONSSupplementSIC5ISpec extends SICSearchHelper {

  val indexName: String = ONS_SUPPLEMENT_SIC5_INDEX

  s"calling GET /search for $indexName index" when {

    def buildQueryAll(query: String, maxResults: Int, page: Int) = buildQuery(query, indexName, Some(maxResults), Some(page), None, queryParser=Some(true))

    "trying to search for a sic code should use the correct url" in {
      val query = "Dairy+farming"
      val client = buildQuery(query, indexName = indexName, queryParser=Some(true))
      client.url shouldBe s"http://localhost:$port/industry-classification-lookup/search?query=$query&indexName=$indexName&queryParser=true"
    }

    "supplying the query 'Dairy+farming' should return a 200 and the sic code descriptions as json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 46,
        "nonFilteredFound" -> 46,
        "results" -> Json.arr(
          Json.obj("code" -> "01410", "desc" -> "Dairy farming"),
          Json.obj("code" -> "01430", "desc" -> "Stud farming"),
          Json.obj("code" -> "01450", "desc" -> "Goat farming")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 28),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 8),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("Dairy+farming", indexName = indexName, Some(3),queryParser=Some(true))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying the query 'Dairy+farming' with the sector query string 'N' should return a 200" +
      "and only the sic code descriptions in sector 'N' as well as all the sector facet counts as json" in {

      val sicCodeLookupResult = Json.obj(
        "numFound" -> 1,
        "nonFilteredFound" -> 46,
        "results" -> Json.arr(
          Json.obj("code" -> "77390", "desc" -> "Dairy machinery rental (non agricultural)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 28),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 9),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 8),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("Dairy+farming", indexName = indexName, Some(5), sector = Some("N"),queryParser=Some(true))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query and requesting page 3 should return a 200 and the sic code descriptions skipping pages 1 & 2" in {

      setupSimpleAuthMocks()

      // get results from the beginning to the end of page 3
      val pages1to3 = buildQueryAll("support", 15, 1).get().json
      val p1to3docs = pages1to3.as[JsObject].value("results").as[JsArray]

      val sicCodeLookupResult = Json.obj(
        "numFound"          -> 48,
        "nonFilteredFound"  -> 48,
        "results" -> Json.arr(
          p1to3docs.value(10),
          p1to3docs.value(11),
          p1to3docs.value(12),
          p1to3docs.value(13),
          p1to3docs.value(14)
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 18),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 12),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 5),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 4),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 4),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "count" -> 2),
          Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 1),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "count" -> 1)
        )
      )

      val client = buildQueryAll("support", 5, 3)

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query with maxResult should return a 200 and fewer sic code descriptions" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 49,
        "nonFilteredFound" -> 49,
        "results" -> Json.arr(
          Json.obj("code" -> "02100", "desc" -> "Silviculture and other forestry activities"),
          Json.obj("code" -> "02400", "desc" -> "Support services to forestry"),
          Json.obj("code" -> "32500", "desc" -> "Foot support (manufacture)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 18),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 12),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 6),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 4),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 4),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "count" -> 2),
          Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 1),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("support silviculture", indexName = indexName, Some(3), queryParser=Some(true))

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

      val client = buildQuery("testtesttest", indexName = indexName, Some(10),queryParser=Some(true))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a query with no journey should default to query builder" in {


      val sicCodeLookupResult = Json.obj(
        "numFound" -> 49,
        "nonFilteredFound" -> 49,
        "results" -> Json.arr(
          Json.obj("code" -> "02100", "desc" -> "Silviculture and other forestry activities"),
          Json.obj("code" -> "02400", "desc" -> "Support services to forestry"),
          Json.obj("code" -> "32500", "desc" -> "Foot support (manufacture)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 18),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 12),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 6),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 4),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 4),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "count" -> 2),
          Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 1),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "count" -> 1)
        )
      )
      val client = buildQuery("support silviculture", indexName = indexName, Some(3),queryParser=None, queryBoostFirstTerm = None)

      setupSimpleAuthMocks()

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult

    }

    "return a valid set of results when using Query booster" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 49,
        "nonFilteredFound" -> 49,
        "results" -> Json.arr(
          Json.obj("code" -> "02400", "desc" -> "Support services to forestry"),
          Json.obj("code" -> "32500", "desc" -> "Foot support (manufacture)"),
          Json.obj("code" -> "32500", "desc" -> "Instep support (manufacture)")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 18),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 12),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 6),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 4),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 4),
          Json.obj("code" -> "J", "name" -> "Information And Communication", "count" -> 2),
          Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 1),
          Json.obj("code" -> "O", "name" -> "Public Administration And Defence; Compulsory Social Security", "count" -> 1),
          Json.obj("code" -> "P", "name" -> "Education", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("support Silviculture", indexName = indexName, Some(3), queryParser = None, queryBoostFirstTerm = Some(true))

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

      val client = buildQuery("testtesttest", indexName = indexName, Some(10),queryParser=Some(true))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query with maxResult should return a 200 and fewer sic code descriptions (FUZZY QUERY)" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 1,
        "nonFilteredFound" -> 1,
        "results" -> Json.arr(
          Json.obj("code" -> "02100", "desc" -> "Silviculture and other forestry activities")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("SiviculturE", indexName = indexName, Some(3), queryParser=None, queryBoostFirstTerm = None)

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query but getting no results and no facets should return the corresponding json (FUZZY QUERY)" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 0,
        "nonFilteredFound" -> 0,
        "results" -> Json.arr(),
        "sectors" -> Json.arr()
      )

      setupSimpleAuthMocks()

      val client = buildQuery("testtesttest", indexName = indexName, Some(10),queryParser=None, queryBoostFirstTerm = None)

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }
  }

}
