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

trait SicIndexSpec extends UnitSpec {

  val indexName: String

  val FIELD_CODE = "code"
  val FIELD_DESC = "description"
  val FIELD_SEARCH_TERMS = "searchTerms"

  val industryCodeMapping = Map("01" -> "A", "02" -> "A", "03" -> "A",
    "05" -> "B", "06" -> "B", "07" -> "B", "08" -> "B", "09" -> "B")

  val stopWords = List(
    "a", "an", "and", "are", "as", "at", "be", "but", "by",
    "for", "if", "in", "into", "is", // "it",
    "no", "not", "of", "on", "or", "such",
    "that", "the", "their", "then", "there", "these",
    "they", "this", "to", "was", "will", "with"
  )

  val stopSet = {
    import scala.collection.JavaConverters._
    new CharArraySet(stopWords.asJava, false)
  }

  val analyzer = new StandardAnalyzer(stopSet)

  lazy val indexPath = FileSystems.getDefault.getPath("target", "scala-2.11", "resource_managed", "main", "conf", "index", indexName)

  private def openIndex() = new NIOFSDirectory(indexPath)

  private def withSearcher(f: IndexSearcher => Unit): Unit = {
    val index: Directory = openIndex()
    val reader: IndexReader = DirectoryReader.open(index)
    val searcher = new IndexSearcher(reader)
    f(searcher)
    index.close()
  }

  private def withIndex(f: Directory => Unit) = {
    val index: Directory = openIndex()
    f(index)
    index.close()
  }

  private def setupFacetedSearch() = {
    val reader: IndexReader = DirectoryReader.open(openIndex())

    val searcher = new IndexSearcher(reader)
    val state = new DefaultSortedSetDocValuesReaderState(reader)

    (searcher, state)
  }

  private def returnIndustrySector(sicCode: String) = {
    val firstTwoChars = sicCode.substring(0, 2)
    industryCodeMapping.getOrElse(firstTwoChars, throw new Exception(s"Industry code for sic-code $sicCode does not exist"))
  }

  def findCorrectSingleResult(data: Map[String, String]): Unit = data foreach {
    case (searchCode, searchDesc) =>
      s"find the correct single result document for $indexName search with $searchCode" in {
        withSearcher {
          searcher =>
            val qp = new QueryParser(FIELD_CODE, analyzer)

            val results = searcher.search(qp.parse(searchCode), 5)

            results.totalHits shouldBe 1

            val result = results.scoreDocs(0)
            val doc = searcher.doc(result.doc)
            val resultCode = doc.get(FIELD_CODE)
            val resultDesc = doc.get(FIELD_DESC)

            resultCode shouldBe searchCode
            resultDesc shouldBe searchDesc
        }
      }
  }

  case class ST(query: String, numMin: Int, topHit: (String, String), expectedDesc: Seq[String])

  def returnSetOffResultsWithTopHit(data: Seq[ST]): Unit = data foreach { searchTerm =>
    s"""return at least ${searchTerm.numMin} result when searching for "${searchTerm.query}"  with a top hit of ${searchTerm.topHit}""" in {
      withSearcher { searcher =>
        val qp = new QueryParser(FIELD_SEARCH_TERMS, analyzer)

        val result = searcher.search(qp.parse(searchTerm.query), searchTerm.numMin)

        result.totalHits.toInt should be >= searchTerm.numMin

        val results = result.scoreDocs.toSeq map {
          result =>
            val doc = searcher.doc(result.doc)
            val code = doc.get(FIELD_CODE)
            val description = doc.get(FIELD_DESC)
            (code, description)
        }

        results.head shouldBe searchTerm.topHit

        // TODO look at improving these checks to assert the results are expected
        if (searchTerm.expectedDesc.nonEmpty) {
          results exists {
            case (_, description) => searchTerm.expectedDesc forall description.contains
          } shouldBe true
        }
      }
    }
  }

  def returnMultiplePagesOfResults(queries: Seq[String]): Unit = queries foreach { queryString =>
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

        page2SearchResult.totalHits shouldBe initialHits
        page2Result shouldBe p2ExpectedResult

        // search for the 3rd page using a collector
        val p3Collector = TopScoreDocCollector.create(1000) // consider whether 1000 is too high
        searcher.search(query, p3Collector)
        val page3SearchResult = p3Collector.topDocs(p3StartIndex, hitsPerPage)
        val page3TopDoc = searcher.doc(page3SearchResult.scoreDocs(0).doc)
        val page3Result = (page3TopDoc.get(FIELD_CODE), page3TopDoc.get(FIELD_DESC))

        page3SearchResult.totalHits shouldBe initialHits
        page3Result shouldBe p3ExpectedResult
      }
    }
  }

  //TODO Evaluate the effectiveness of the next three tests
  Seq(
    "01120",
    "01400"
  ) foreach { sicCode =>
    s"""return the correct industry sector of A for sic code "${sicCode}" """ in {
      val result = returnIndustrySector(sicCode)
      result shouldBe "A"
    }
  }

  Seq(
    "05101",
    "08910"
  ) foreach { sicCode =>
    s"""return the correct industry sector of B for sic code "$sicCode" """ in {
      val result = returnIndustrySector(sicCode)
      result shouldBe "B"
    }
  }

  "throw an exception when a sic-code with no corresponding industry sector is used" in {
    val ex = intercept[Exception](await(returnIndustrySector("99999999")))
    ex.getMessage shouldBe "Industry code for sic-code 99999999 does not exist"
  }
  // end review

  def createIndexOnFacetedFields(query: String, expectedResult: Seq[(String, Int)]): Unit = {
    "created an index with faceted fields" in {

      val (searcher, state) = setupFacetedSearch()

      val collector = new FacetsCollector()

      val searchTerm: Query = new TermQuery(new Term(FIELD_SEARCH_TERMS, query))

      FacetsCollector.search(searcher, searchTerm, 18, collector)

      val facets = new SortedSetDocValuesFacetCounts(state, collector)

      val facetResult = facets.getTopChildren(18, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      val resultsCount = expectedResult.map(_._2).sum

      facetResult.toSeq shouldBe expectedResult

      withSearcher { ws =>
        val results: TopDocs = ws.search(searchTerm, 20)
        results.totalHits shouldBe resultsCount
      }
    }
  }

}
