
package helpers

trait SICSearchHelper extends IntegrationSpecBase {

  def buildQuery(query: String,
                 indexName: String,
                 maxResults: Option[Int] = None,
                 page: Option[Int] = None,
                 sector: Option[String] = None,
                 queryParser: Option[Boolean] = None,
                 queryBoostFirstTerm: Option[Boolean] = None,
                 lang: String = "en") = {
    val maxParam = maxResults.fold("")(n => s"&pageResults=$n")
    val indexNameParam = s"&indexName=$indexName"
    val pageParam = page.fold("")(n => s"&page=$n")
    val sectorParam = sector.fold("")(s => s"&sector=$s")
    val queryParserParam = queryParser.fold("")(s => s"&queryParser=$s")
    val queryBoostFirstTermParam = queryBoostFirstTerm.fold("")(s => s"&queryBoostFirstTerm=$s")
    buildClient(s"/search?query=$query$indexNameParam$maxParam$pageParam$sectorParam$queryParserParam$queryBoostFirstTermParam&lang=$lang")
  }

}
