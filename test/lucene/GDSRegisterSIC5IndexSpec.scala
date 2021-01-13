/*
 * Copyright 2021 HM Revenue & Customs
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

import helpers.SICIndexSpec
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{Query, TermQuery, TopDocs, TopScoreDocCollector}
import services.Indexes.GDS_REGISTER_SIC5_INDEX

class GDSRegisterSIC5IndexSpec extends SICIndexSpec {
  val indexName: String = GDS_REGISTER_SIC5_INDEX

  "GDS Sic search" should {
    Map(
      "01000" -> "Crop and animal production, hunting and related service activities",
      "01100" -> "Growing of non-perennial crops",
      "01110" -> "Growing of cereals (except rice), leguminous crops and oil seeds",
      "01120" -> "Growing of rice",
      "99000" -> "Activities of extraterritorial organisations and bodies"
    ).foreach {
      case (searchCode, searchDesc) =>
        s"find the correct single result document for $indexName search with $searchCode" in {
          withSearcher {
            searcher =>
              val qp = new QueryParser(FIELD_CODE, analyzer)

              val results = searcher.search(qp.parse(searchCode), 5)

              results.totalHits mustBe 1

              val result = results.scoreDocs(0)
              val doc = searcher.doc(result.doc)
              val resultCode = doc.get(FIELD_CODE)
              val resultDesc = doc.get(FIELD_DESC)

              resultCode mustBe searchCode
              resultDesc mustBe searchDesc
          }
        }
    }

    Seq(
      ST("Crop and animal production, hunting and related service activities", 1,
        ("01000", "Crop and animal production, hunting and related service activities"), Seq("activities")),
      ST("Growing of rice", 4, ("01120", "Growing of rice"), Seq("Growing"))
    ).foreach { searchTerm =>
      s"""return at least ${searchTerm.numMin} result when searching for "${searchTerm.query}"  with a top hit of ${searchTerm.topHit}""" in {
        withSearcher { searcher =>
          val qp = new QueryParser(FIELD_SEARCH_TERMS, analyzer)

          val result = searcher.search(qp.parse(searchTerm.query), searchTerm.numMin)

          result.totalHits.toInt must be >= searchTerm.numMin

          val results = result.scoreDocs.toSeq map {
            result =>
              val doc = searcher.doc(result.doc)
              val code = doc.get(FIELD_CODE)
              val description = doc.get(FIELD_DESC)
              (code, description)
          }

          results.head mustBe searchTerm.topHit

          // TODO look at improving these checks to assert the results are expected
          if (searchTerm.expectedDesc.nonEmpty) {
            results exists {
              case (_, description) => searchTerm.expectedDesc forall description.contains
            } mustBe true
          }
        }
      }
    }

    Seq(
      "Support",
      "Growing",
      "activities"
    ).foreach { queryString =>
      s"""return page 2 & 3 results when searching for "$queryString" """ in {
        withSearcher { searcher =>
          val hitsPerPage = 5
          val p2StartIndex = hitsPerPage
          val p3StartIndex = 2 * hitsPerPage

          val qp = new QueryParser(FIELD_SEARCH_TERMS, analyzer)
          val query = qp.parse(queryString)

          // search for the first page with an extra item (i.e. first of page 2)
          val resultPage1Plus1 = searcher.search(query, (2 * hitsPerPage) + 1)

          val initialHits = resultPage1Plus1.totalHits
          val p2ExpectedDoc = searcher.doc(resultPage1Plus1.scoreDocs(hitsPerPage).doc)
          val p2ExpectedResult = (p2ExpectedDoc.get(FIELD_CODE), p2ExpectedDoc.get(FIELD_DESC))
          val p3ExpectedDoc = searcher.doc(resultPage1Plus1.scoreDocs(2 * hitsPerPage).doc)
          val p3ExpectedResult = (p3ExpectedDoc.get(FIELD_CODE), p3ExpectedDoc.get(FIELD_DESC))

          // search for the 2nd page using a collector
          val p2Collector = TopScoreDocCollector.create(1000) // consider whether 1000 is too high
          searcher.search(query, p2Collector)
          val page2SearchResult = p2Collector.topDocs(p2StartIndex, hitsPerPage)
          val page2TopDoc = searcher.doc(page2SearchResult.scoreDocs(0).doc)
          val page2Result = (page2TopDoc.get(FIELD_CODE), page2TopDoc.get(FIELD_DESC))

          page2SearchResult.totalHits mustBe initialHits
          page2Result mustBe p2ExpectedResult

          // search for the 3rd page using a collector
          val p3Collector = TopScoreDocCollector.create(1000) // consider whether 1000 is too high
          searcher.search(query, p3Collector)
          val page3SearchResult = p3Collector.topDocs(p3StartIndex, hitsPerPage)
          val page3TopDoc = searcher.doc(page3SearchResult.scoreDocs(0).doc)
          val page3Result = (page3TopDoc.get(FIELD_CODE), page3TopDoc.get(FIELD_DESC))

          page3SearchResult.totalHits mustBe initialHits
          page3Result mustBe p3ExpectedResult
        }
      }
    }

    "created an index with faceted fields" in {

      val (searcher, state) = setupFacetedSearch()

      val collector = new FacetsCollector()

      val searchTerm: Query = new TermQuery(new Term(FIELD_SEARCH_TERMS, "support"))

      FacetsCollector.search(searcher, searchTerm, 18, collector)

      val facets = new SortedSetDocValuesFacetCounts(state, collector)

      val facetResult = facets.getTopChildren(18, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      val expectedResult = Seq("N" -> 6, "A" -> 5, "B" -> 3, "H" -> 3, "P" -> 1, "R" -> 1)
      val resultsCount = expectedResult.map(_._2).sum

      facetResult mustBe expectedResult

      withSearcher { ws =>
        val results: TopDocs = ws.search(searchTerm, 20)
        results.totalHits mustBe resultsCount
      }
    }
  }
}
