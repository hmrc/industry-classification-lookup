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

import java.nio.file.FileSystems

import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.sortedset.{DefaultSortedSetDocValuesReaderState, SortedSetDocValuesFacetCounts}
import org.apache.lucene.index.{DirectoryReader, IndexReader, Term}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, NIOFSDirectory}
import uk.gov.hmrc.play.test.UnitSpec

class GDSSicIndexSpec extends SicIndexSpec {
  val indexName: String = "gds"

  "GDS Sic search" should {
    findCorrectSingleResult(Map(
      "01000" -> "Crop and animal production, hunting and related service activities",
      "01100" -> "Growing of non-perennial crops",
      "01110" -> "Growing of cereals (except rice), leguminous crops and oil seeds",
      "01120" -> "Growing of rice",
      "99000" -> "Activities of extraterritorial organisations and bodies"
    ))

    returnSetOffResultsWithTopHit(Seq(
      ST("Crop and animal production, hunting and related service activities", 1,
        ("01000", "Crop and animal production, hunting and related service activities"), Seq("activities")),
      ST("Growing of rice", 4, ("01120", "Growing of rice"), Seq("Growing"))
    ))

    returnMultiplePagesOfResults(Seq(
      "Support",
      "Growing",
      "activities"
    ))

    createIndexOnFacetedFields("support", Seq("N" -> 6, "A" -> 5, "B" -> 3, "H" -> 3, "P" -> 1, "R" -> 1))
  }
}
