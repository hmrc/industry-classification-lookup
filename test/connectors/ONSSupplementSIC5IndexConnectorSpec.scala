/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors

import config.ICLConfig
import models.SicCode
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.Mode.Mode
import services.{QueryType, SearchResult}
import uk.gov.hmrc.play.test.UnitSpec

class ONSSupplementSIC5IndexConnectorSpec extends UnitSpec with MockitoSugar {

  val mockConfig: ICLConfig = new ICLConfig {
    override protected def mode: Mode = ???
    override protected def runModeConfiguration: Configuration = ???
    override def getConfigString(x: String) = "target/scala-2.11/resource_managed/main/conf/index"
  }

  trait Setup {
    val onsIndex = new ONSSupplementSIC5IndexConnectorImpl(mockConfig)
    val gdsIndex = new GDSRegisterSIC5IndexConnectorImpl(mockConfig)
  }

  val journey: String = QueryType.QUERY_BUILDER

  "lookup" should {

    Map(
      "01410" -> "Raising of dairy cattle",
      "01130" -> "Growing of vegetables and melons, roots and tubers",
      "28110" -> "Manufacture of engines and turbines, except aircraft, vehicle and cycle engines",
      "28930" -> "Manufacture of machinery for food, beverage and tobacco processing"
    ) foreach {
      case ((searchCode, searchDesc)) =>
        s"find the correct single result document for code8 search with $searchCode" in new Setup {

          val result: Option[SicCode] = gdsIndex.lookup(searchCode)

          result shouldBe defined

          result.get shouldBe SicCode(searchCode, searchDesc)
        }
    }

    "Return None if the sic code isn't found" in new Setup {
      onsIndex.lookup("ABCDE") shouldBe None
    }

    // TODO - H2 test the multi-return scenario without altering the index
  }

  "search" should {

    case class ST(query: String, numMin: Int, topHit: SicCode)

    Seq(
      ST("Dairy farming", 1, SicCode("01410", "Dairy farming"))
    ) foreach { data =>
      s"""should return at least ${data.numMin} result when searching for "${data.query}"  with a top hit of ${data.topHit}""" in new Setup {

        val result: SearchResult = onsIndex.search(data.query, queryType = Some(journey))

        val sics: Seq[SicCode] = result.results
        sics.length should be >= data.numMin

        sics.head shouldBe data.topHit

        sics shouldBe Seq(
          SicCode("01410", "Dairy farming"),
          SicCode("01430", "Stud farming"),
          SicCode("01450", "Goat farming"),
          SicCode("01450", "Sheep farming"),
          SicCode("01460", "Pig farming")
        )
      }
    }

    Seq(
      "Dairy farming" -> 3,
      "Dairy farming" -> 1,
      "Dairy farming" -> 10
    ) foreach { case (query, maxResults) =>
      s"""should return $maxResults results when maxResult is specified when searching for "$query"""" in new Setup {

        val result: SearchResult = onsIndex.search(query, maxResults, queryType = Some(journey))

        val sics: Seq[SicCode] = result.results
        sics.length shouldBe maxResults
      }
    }

    "should return 2 results from page 2" in new Setup {

      val query = "Dairy farming"
      val pageResults = 2
      val page = 2

      val result: SearchResult = onsIndex.search(query, pageResults, page, None, Some(journey))

      val sics: Seq[SicCode] = result.results
      sics.length shouldBe pageResults

      sics shouldBe Seq(
        SicCode("01450", "Goat farming"),
        SicCode("01450", "Sheep farming")
      )
    }

    "should return 2 results from page 1 when specifying a negative page" in new Setup {

      val query = "Dairy farming"
      val pageResults = 2
      val page: Int = -1

      val result: SearchResult = onsIndex.search(query, pageResults, page, None, Some(journey))

      val sics: Seq[SicCode] = result.results
      sics.length shouldBe pageResults

      sics shouldBe Seq(
        SicCode("01410", "Dairy farming"),
        SicCode("01430", "Stud farming")
      )
    }

    Seq(
      ST("Cress", 1, SicCode("01130", "Cress growing"))
    ) foreach { data =>
      s"""should return at least ${data.numMin} result when searching for "${data.query}"  with a top hit of ${data.topHit}""" in new Setup {

        val result: SearchResult = onsIndex.search(data.query, queryType = Some(journey))

        val sics: Seq[SicCode] = result.results

        sics.length should be >= data.numMin

        sics.head shouldBe data.topHit

        sics shouldBe Seq(
          SicCode("01130", "Cress growing")
        )
      }
    }

    "Should perform second search with a fuzzy match if first search has no match" in new Setup {
      val result: SearchResult = onsIndex.search("XXX", queryType = Some(journey))
      result.results shouldBe Seq(
        SicCode("20412", "Wax (manufacture)"),
        SicCode("25730", "Axe (manufacture)"),
        SicCode("69203", "Tax consultancy"),
        SicCode("16240", "Box pallet (manufacture)"),
        SicCode("19201", "Paraffin wax (manufacture)")
      )
    }

    "Should return nothing if first search and second search with fuzzy match has no match" in new Setup {
      val result: SearchResult = onsIndex.search("XXXX", queryType = Some(journey))
      result.results shouldBe Seq()
    }
  }

  "searching with QueryBooster" should {

    val journey: String = QueryType.QUERY_BOOSTER

    "return different top results for dairy + farming and farming + dairy " in new Setup {
      val dairyFarmingResults = onsIndex.search("dairy farming", queryType = Some(journey))
      val farmingDairyResults = onsIndex.search("farming dairy", queryType = Some(journey))

      dairyFarmingResults.results.take(3) shouldNot   be(farmingDairyResults.results.take(3))
      dairyFarmingResults.results.length  shouldEqual farmingDairyResults.results.length
    }
  }
}
