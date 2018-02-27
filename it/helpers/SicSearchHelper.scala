
package helpers

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import services.QueryType.QUERY_PARSER

trait SicSearchHelper extends IntegrationSpecBase {

  val indexName: String

  protected def buildQuery(query: String, maxResults: Option[Int] = None, page: Option[Int] = None, sector: Option[String] = None, queryType: Option[String]) = {
    val maxParam = maxResults.fold("")(n => s"&pageResults=$n")
    val indexNameParam = s"&indexName=$indexName"
    val pageParam = page.fold("")(n => s"&page=$n")
    val sectorParam = sector.fold("")(s => s"&sector=$s")
    val queryTypeParam = queryType.fold("")(s => s"&queryType=$s")
    buildClient(s"/search?query=$query$indexNameParam$maxParam$pageParam$sectorParam$queryTypeParam")
  }

  def buildQueryTupled(maxResults: Option[Int] = None, page: Option[Int] = None, sector: Option[String] = None, queryType: Option[String])(query: String) = {
    val maxParam = maxResults.fold("")(n => s"&pageResults=$n")
    val indexNameParam = s"&indexName=$indexName"
    val pageParam = page.fold("")(n => s"&page=$n")
    val sectorParam = sector.fold("")(s => s"&sector=$s")
    val queryTypeParam = queryType.fold("")(s => s"&queryType=$s")
    buildClient(s"/search?query=$query$indexNameParam$maxParam$pageParam$sectorParam$queryTypeParam")
  }

  def useCorrectUrlInSearch(query: String, qType: String) = {
    "trying to search for a sic code should use the correct url" in {
      val client = buildQuery(query, queryType = Some(qType))
      client.url shouldBe s"http://localhost:$port/industry-classification-lookup/search?query=$query&indexName=$indexName&queryType=$qType"
    }
  }

  def getPage3ofResults(query: String, expectedResult: JsObject) = {
    "supplying a valid query and requesting page 3 should return a 200 and the sic code descriptions skipping pages 1 & 2" in {

      setupSimpleAuthMocks()

      // get results from the beginning to the end of page 3
      val pages1to3 = buildQuery(query, maxResults = Some(15), queryType = Some(QUERY_PARSER)).get().json
      val p1to3docs = pages1to3.as[JsObject].value("results").as[JsArray]

      val sicCodeLookupResult = expectedResult ++ Json.obj("results" -> Json.arr(
        p1to3docs.value(10),
        p1to3docs.value(11),
        p1to3docs.value(12),
        p1to3docs.value(13),
        p1to3docs.value(14)
      ))

      val client = buildQuery(query, maxResults = Some(5), page = Some(3), queryType = Some(QUERY_PARSER))

      val response: WSResponse = client.get()

      response.status shouldBe 200
      response.json shouldBe sicCodeLookupResult
    }
  }

  def returnSicSearchJson(query: String, expectedResult: JsObject, client: String => WSRequest) = {
    s"supplying the query '$query' should return a 200 and the sic code descriptions as json" in {
      setupSimpleAuthMocks()

      val response: WSResponse = client(query).get()

      response.status shouldBe 200
      response.json shouldBe expectedResult
    }
  }

  def returnSicCodeFromSector(query: String, sector: String, expectedResult: JsObject, client: String => WSRequest) = {
    s"supplying the query '$query' with the sector query string '$sector' should return a 200" +
      s"and only the sic code descriptions in sector '$sector' as well as all the sector facet counts as json" in {
      setupSimpleAuthMocks()


      val response: WSResponse = client(query).get()

      response.status shouldBe 200
      response.json shouldBe expectedResult
    }
  }

  def returnResultsWithLimits(query: String, expectedResult: JsObject, client: String => WSRequest) = {
    "supplying a valid query with maxResult should return a 200 and fewer sic code descriptions" in {

      setupSimpleAuthMocks()

      val response: WSResponse = client(query).get()

      response.status shouldBe 200
      response.json shouldBe expectedResult
    }
  }

  def returnNoResultOrFacetInValidSearch(query: String, expectedResult: JsObject, client: String => WSRequest) = {
    s"supplying a valid query of '$query' but getting no results and no facets should return the corresponding json" in {

      setupSimpleAuthMocks()

      val response: WSResponse = client(query).get()

      response.status shouldBe 200
      response.json shouldBe expectedResult
    }
  }

  def errorIfJourneyTypeIsInvalid(client: WSRequest) = {
    "supplying a query with no journey should error" in {

      setupSimpleAuthMocks()

      val response: WSResponse = client.get()

      response.status shouldBe 500

    }
  }

}
