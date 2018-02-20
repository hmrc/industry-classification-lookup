
package api

import helpers.SicSearchHelper
import play.api.libs.json.Json
import services.QueryType.QUERY_PARSER


class SearchONSISpec extends SicSearchHelper {

  val query: String     = "Dairy+farming"
  val indexName: String = "ons"

  val sicCodePage3Facets = Json.obj(
    "numFound"          -> 48,
    "nonFilteredFound"  -> 48,
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

  val sicCodeLookupResultFromSector = Json.obj(
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

  val sicCodeManyResults  = Json.obj(
    "numFound" -> 48,
    "nonFilteredFound" -> 48,
    "results" -> Json.arr(
      Json.obj("code" -> "02400", "desc" -> "Support services to forestry"),
      Json.obj("code" -> "32500", "desc" -> "Foot support (manufacture)"),
      Json.obj("code" -> "32500", "desc" -> "Instep support (manufacture)")
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

  val sicCodeNoResult = Json.obj(
    "numFound" -> 0,
    "nonFilteredFound" -> 0,
    "results" -> Json.arr(),
    "sectors" -> Json.arr()
  )


  "searching using the ons data set" should {
    useCorrectUrlInSearch(query, QUERY_PARSER)

    getPage3ofResults("support", sicCodePage3Facets)

    returnSicSearchJson(query, sicCodeLookupResult, buildQueryTupled(Some(3), queryType = Some(QUERY_PARSER)))

    returnSicCodeFromSector(query, "N", sicCodeLookupResultFromSector, buildQueryTupled(Some(5), sector = Some("N"), queryType = Some(QUERY_PARSER)))

    returnResultsWithLimits("support", sicCodeManyResults, buildQueryTupled(Some(3), queryType = Some(QUERY_PARSER)))

    returnNoResultOrFacetInValidSearch("testtesttesttest", sicCodeNoResult, buildQueryTupled(Some(10), queryType = Some(QUERY_PARSER)))

    errorIfJourneyTypeIsInvalid(buildQuery("testesttestttes", Some(3), queryType = Some("SomeIncorrectThing")))
  }
}
