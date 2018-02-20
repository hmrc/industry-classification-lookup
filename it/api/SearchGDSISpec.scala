
package api

import helpers.SicSearchHelper
import play.api.libs.json.Json
import services.Indexes.GDS_INDEX
import services.QueryType.QUERY_PARSER

class SearchGDSISpec extends SicSearchHelper {

  val query = "Dairy+farming"
  val indexName: String = GDS_INDEX

  val sicCodeLookupResult = Json.obj(
    "numFound" -> 4,
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

  val sicCodeLookupResultFromSector = Json.obj(
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

  val sicCodeManyResults  = Json.obj(
    "numFound" -> 19,
    "nonFilteredFound" -> 19,
    "results" -> Json.arr(
      Json.obj("code" -> "02400", "desc" -> "Support services to forestry"),
      Json.obj("code" -> "52200", "desc" -> "Support activities for transportation"),
      Json.obj("code" -> "85600", "desc" -> "Educational support activities")
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

  val sicCodePage3Facets = Json.obj(
    "numFound" -> 19,
    "nonFilteredFound" -> 19,
    "sectors" -> Json.arr(
      Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "count" -> 6),
      Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "count" -> 5),
      Json.obj("code" -> "B", "name" -> "Mining And Quarrying", "count" -> 3),
      Json.obj("code" -> "H", "name" -> "Transportation And Storage", "count" -> 3),
      Json.obj("code" -> "P", "name" -> "Education", "count" -> 1),
      Json.obj("code" -> "R", "name" -> "Arts, Entertainment And Recreation", "count" -> 1)
    )
  )

  val sicCodeNoResult = Json.obj(
    "numFound" -> 0,
    "nonFilteredFound" -> 0,
    "results" -> Json.arr(),
    "sectors" -> Json.arr()
  )

  "Search using the GDS data source" should {
    useCorrectUrlInSearch(query, QUERY_PARSER)

    getPage3ofResults("support", sicCodePage3Facets)

    returnSicSearchJson(query, sicCodeLookupResult, buildQueryTupled(Some(3), queryType = Some(QUERY_PARSER)))

    returnSicCodeFromSector(query, "G", sicCodeLookupResultFromSector, buildQueryTupled(Some(5), sector = Some("G"), queryType = Some(QUERY_PARSER)))

    returnResultsWithLimits("support", sicCodeManyResults, buildQueryTupled(Some(3), queryType = Some(QUERY_PARSER)))

    returnNoResultOrFacetInValidSearch("testtesttesttest", sicCodeNoResult, buildQueryTupled(Some(10), queryType = Some(QUERY_PARSER)))

    errorIfJourneyTypeIsInvalid(buildQuery("testesttestttes", Some(3), queryType = Some("SomeIncorrectThing")))
  }


}
