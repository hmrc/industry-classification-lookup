
package api

import helpers.SICSearchHelper
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSResponse
import services.Indexes.GDS_REGISTER_SIC5_INDEX
import services.QueryType._

class SearchGDSRegisterSIC5ISpec extends SICSearchHelper {

  val indexName: String = GDS_REGISTER_SIC5_INDEX

  s"calling GET /search for $indexName index" when {


    def buildQuery(query: String, indexName: String, maxResults: Option[Int] = None, page: Option[Int] = None, sector: Option[String] = None, queryType: Option[String]) = {
      val maxParam = maxResults.fold("")(n => s"&pageResults=$n")
      val indexNameParam = s"&indexName=$indexName"
      val pageParam = page.fold("")(n => s"&page=$n")
      val sectorParam = sector.fold("")(s => s"&sector=$s")
      val queryTypeParam = queryType.fold("")(s => s"&queryType=$s")
      buildClient(s"/search?query=$query$indexNameParam$maxParam$pageParam$sectorParam$queryTypeParam")
    }

    def buildQueryAll(query: String, maxResults: Int, page: Int) = buildQuery(query, indexName, Some(maxResults), Some(page), None, Some(QUERY_PARSER))

    "trying to search for a sic code should use the correct url" in {
      val query = "Dairy+farming"
      val client = buildQuery(query, indexName = indexName, queryType=Some(QUERY_PARSER))
      client.url shouldBe s"http://localhost:$port/industry-classification-lookup/search?query=$query&indexName=$indexName&queryType=$QUERY_PARSER"
    }

    "supplying the query 'Dairy+farming' should return a 200 and the sic code descriptions as json" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 3,
        "nonFilteredFound" -> 4,
        "results" -> Json.arr(
          Json.obj("code" -> "01500", "desc" -> "Mixed farming"),
          Json.obj("code" -> "01410", "desc" -> "Raising of dairy cattle"),
          Json.obj("code" -> "10500", "desc" -> "Manufacture of dairy products")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 2),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 1),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("Dairy+farming", indexName = indexName, Some(3),queryType=Some(QUERY_PARSER))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying the query 'Dairy+farming' with the sector query string 'G' should return a 200" +
      "and only the sic code descriptions in sector 'G' as well as all the sector facet counts as json" in {

      val sicCodeLookupResult = Json.obj(
        "numFound" -> 1,
        "nonFilteredFound" -> 4,
        "results" -> Json.arr(
          Json.obj("code" -> "46330", "desc" -> "Wholesale of dairy products, eggs and edible oils and fats")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 2),
          Json.obj("code" -> "C", "name" -> "Manufacturing", "count" -> 1),
          Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("Dairy+farming", indexName = indexName, Some(5), sector = Some("G"),queryType=Some(QUERY_PARSER))

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
        "numFound" -> 5,
        "nonFilteredFound" -> 19,
        "results" -> Json.arr(
          p1to3docs.value(10),
          p1to3docs.value(11),
          p1to3docs.value(12),
          p1to3docs.value(13),
          p1to3docs.value(14)
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 6),
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 5),
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 3),
          Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 3),
          Json.obj("code" -> "P", "name" -> "Education", "count" -> 1),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 1)
        )
      )

      val client = buildQueryAll("support", 5, 3)

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a valid query with maxResult should return a 200 and fewer sic code descriptions" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 3,
        "nonFilteredFound" -> 20,
        "results" -> Json.arr(
          Json.obj("code" -> "02100", "desc" -> "Silviculture and other forestry activities"),
          Json.obj("code" -> "02400", "desc" -> "Support services to forestry"),
          Json.obj("code" -> "52200", "desc" -> "Support activities for transportation")
        ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 6),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 6),
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 3),
          Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 3),
          Json.obj("code" -> "P", "name" -> "Education", "count" -> 1),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("support silviculture", indexName = indexName, Some(3), queryType=Some(QUERY_BUILDER))

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

      val client = buildQuery("testtesttest", indexName = indexName, Some(10),queryType=Some(QUERY_PARSER))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }

    "supplying a query with no journey should error" in {

      val client = buildQuery("testtesttest", indexName = indexName, Some(10),queryType=Some("RubbishJourney"))

      setupSimpleAuthMocks()

      val response: WSResponse = client.get()

      response.status shouldBe 500

    }

    "return a valid set of results when using Query booster" in {
      val sicCodeLookupResult = Json.obj(
        "numFound" -> 5,
        "nonFilteredFound" -> 20,
        "results" ->  Json.arr(
          Json.obj("code" -> "02400", "desc" -> "Support services to forestry"),
          Json.obj("code" -> "52200", "desc" -> "Support activities for transportation"),
          Json.obj("code" -> "85600", "desc" -> "Educational support activities"),
          Json.obj("code" -> "82000", "desc" -> "Office administrative, office support and other business support activities"),
          Json.obj("code" -> "01610", "desc" -> "Support activities for crop production")
      ),
        "sectors" -> Json.arr(
          Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 6),
          Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 6),
          Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 3),
          Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 3),
          Json.obj("code" -> "P", "name" -> "Education", "count" -> 1),
          Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 1)
        )
      )

      setupSimpleAuthMocks()

      val client = buildQuery("Support silviculture", indexName = indexName, Some(5), queryType=Some(QUERY_BOOSTER))

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

      val client = buildQuery("testtesttest", indexName = indexName, Some(10),queryType=Some(QUERY_BOOSTER))

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

      val client = buildQuery("sivicULture", indexName = indexName, Some(3), queryType=Some(FUZZY_QUERY))

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

      val client = buildQuery("testtesttest", indexName = indexName, Some(10),queryType=Some(FUZZY_QUERY))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }
  }
}
