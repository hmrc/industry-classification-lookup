
package helpers

trait SICSearchHelper extends IntegrationSpecBase {

  def buildQuery(query: String, indexName: String, maxResults: Option[Int] = None, page: Option[Int] = None, sector: Option[String] = None, queryType: Option[String]) = {
    val maxParam = maxResults.fold("")(n => s"&pageResults=$n")
    val indexNameParam = s"&indexName=$indexName"
    val pageParam = page.fold("")(n => s"&page=$n")
    val sectorParam = sector.fold("")(s => s"&sector=$s")
    val queryTypeParam = queryType.fold("")(s => s"&queryType=$s")
    buildClient(s"/search?query=$query$indexNameParam$maxParam$pageParam$sectorParam$queryTypeParam")
  }

}
