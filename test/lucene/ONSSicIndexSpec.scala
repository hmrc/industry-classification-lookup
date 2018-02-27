/*
 * Copyright 2018 HM Revenue & Customs
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

package lucene

class ONSSicIndexSpec extends SicIndexSpec {
  val indexName = "ons"

  "ONS Sic Search" should {

    //not valid at the moment as there are multiple results for the same sic code
    "find single result" ignore {
      findCorrectSingleResult(
        Map(
          "01110" -> "Growing of cereals (except rice), leguminous crops and oil seeds",
          "01120" -> "Growing of rice",
          "99000" -> "Activities of extraterritorial organisations and bodies",
          "01240" -> "Growing of pome fruits and stone fruits"
        )
      )
    }

    returnSetOffResultsWithTopHit(Seq(
      ST("Sloe growing", 4,
        ("01240", "Sloe growing"), Seq("growing")),
      ST("Growing of cereals (except rice), leguminous crops and oil seeds", 4,
        ("01110", "Growing of cereals (except rice), leguminous crops and oil seeds"), Seq("Growing")),
      ST("Growing of rice", 4, ("01120", "Growing of rice"), Seq("Growing"))
    ) )

    returnMultiplePagesOfResults(Seq(
      "Support",
      "Growing",
      "activities"
    ))

    createIndexOnFacetedFields("support", Seq("B" -> 18, "C" -> 12, "A" -> 5, "N" -> 4, "R" -> 4, "J" -> 2, "H" -> 1, "O" -> 1, "P" -> 1))
  }

}
