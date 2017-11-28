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

package services

import config.MicroserviceConfig
import models.SicCode
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class SIC8IndexConnectorSpec extends UnitSpec with MockitoSugar {

  val mockConfig: MicroserviceConfig = mock[MicroserviceConfig]

  trait Setup {
    val index: SIC8IndexConnector = new SIC8IndexConnector {
      override val config: MicroserviceConfig = mockConfig
    }
  }

  "lookup" should {

    Map(
      "01410003" -> "Dairy farming",
      "01130024" -> "Sugar beet growing",
      "28110023" -> "Engines for marine use (manufacture)",
      "28930070" -> "Press for food and drink (manufacture)"
    ) foreach {
      case ((searchCode, searchDesc)) =>
        s"find the correct single result document for code8 search with ${searchCode}" in new Setup {

          val result = index.lookup(searchCode)

          result shouldBe defined

          result.get shouldBe SicCode(searchCode, searchDesc)
        }
    }

    "Return None if the sic code isn't found" in new Setup {
      index.lookup("ABCDE") shouldBe None
    }

    // TODO - H2 test the multi-return scenario without altering the index
  }

  "search" should {

    case class ST(query: String, numMin: Int, topHit: SicCode)

    Seq(
      ST("Dairy farming", 1, SicCode("01410003", "Dairy farming"))
    ) foreach { data =>
      s"""should return at least ${data.numMin} result when searching for "${data.query}"  with a top hit of ${data.topHit}""" in new Setup {

        val result = index.search(data.query)

        val sics = result.results
        sics.length should be >= data.numMin

        sics(0) shouldBe data.topHit

        sics shouldBe Seq(
          SicCode("01410003", "Dairy farming"),
          SicCode("01420003", "Cattle farming"),
          SicCode("03220009", "Frog farming"),
          SicCode("01490008", "Fur farming"),
          SicCode("01490026", "Snail farming")
        )
      }
    }

    Seq(
      "Dairy farming" -> 3,
      "Dairy farming" -> 1,
      "Dairy farming" -> 10
    ) foreach { case (query, maxResults) =>
      s"""should return ${maxResults} results when maxResult is specified when searching for "${query}"""" in new Setup {

        val result = index.search(query, maxResults)

        val sics = result.results
        sics.length shouldBe maxResults
      }
    }

    s"""should return 2 results from page 2""" in new Setup {

      val query = "Dairy farming"
      val pageResults = 2
      val page = 2

      val result = index.search(query, pageResults, page)

      val sics = result.results
      sics.length shouldBe pageResults

      sics shouldBe Seq(
        SicCode("03220009", "Frog farming"),
        SicCode("01490008", "Fur farming")
      )
    }

    s"""should return 2 results from page 1 when specifying a negative page""" in new Setup {

      val query = "Dairy farming"
      val pageResults = 2
      val page = -1

      val result = index.search(query, pageResults, page)

      val sics = result.results
      sics.length shouldBe pageResults

      sics shouldBe Seq(
        SicCode("01410003", "Dairy farming"),
        SicCode("01420003", "Cattle farming")
      )
    }

    Seq(
      ST("Cress", 1, SicCode("01130008", "Cress growing"))
    ) foreach { data =>
      s"""should return at least ${data.numMin} result when searching for "${data.query}"  with a top hit of ${data.topHit}""" in new Setup {

        val result = index.search(data.query)

        val sics = result.results

        sics.length should be >= data.numMin

        sics(0) shouldBe data.topHit

        sics shouldBe Seq(
          SicCode("01130008", "Cress growing")
        )
      }
    }

    "Should return nothing if no match" in new Setup {
      val result = index.search("XXX")

      result.results shouldBe Seq()
    }
  }
}
