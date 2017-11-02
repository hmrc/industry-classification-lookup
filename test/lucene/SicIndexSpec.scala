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

package lucene

import java.nio.file.{FileSystems, Path}

import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, NIOFSDirectory}
import uk.gov.hmrc.play.test.UnitSpec


class SicIndexSpec extends UnitSpec {

  "SIC search" should {
    val FIELD_CODE8 = "code8"
    val FIELD_DESC = "description"

    val stopWords = List(
      "a", "an", "and", "are", "as", "at", "be", "but", "by",
      "for", "if", "in", "into", "is", // "it",
      "no", "not", "of", "on", "or", "such",
      "that", "the", "their", "then", "there", "these",
      "they", "this", "to", "was", "will", "with"
    )
    val stopSet = {
      import scala.collection.JavaConverters._
      new CharArraySet(stopWords.asJava, false);
    }

    val analyzer = new StandardAnalyzer(stopSet);

    def openIndex() = {
      val path: Path = FileSystems.getDefault().getPath("conf", "index")
      val index: Directory = new NIOFSDirectory(path);
      index
    }

    def withSearcher(f: IndexSearcher => Unit) = {
      val path: Path = FileSystems.getDefault().getPath("conf", "index")
      val index: Directory = new NIOFSDirectory(path);
      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)
      f(searcher)
      index.close()
    }

    def withIndex(f: Directory => Unit) = {
      val path: Path = FileSystems.getDefault().getPath("conf", "index")
      val index: Directory = new NIOFSDirectory(path);
      f(index)
      index.close()
    }

    Map(
      "01410003" -> "Dairy farming",
      "01130024" -> "Sugar beet growing",
      "28110023" -> "Engines for marine use (manufacture)",
      "28930070" -> "Press for food and drink (manufacture)"
    ) foreach {
      case ((searchCode, searchDesc)) =>
        s"find the correct single result document for code8 search with ${searchCode}" in {
          withSearcher {
            searcher =>
              val qp = new QueryParser(FIELD_CODE8, analyzer) // TODO QueryBuilder?

              val results = searcher.search(qp.parse(searchCode), 5)

              results.totalHits shouldBe 1

              val result = results.scoreDocs(0)
              val doc = searcher.doc(result.doc)
              val resultCode = doc.get(FIELD_CODE8)
              val resultDesc = doc.get(FIELD_DESC)

              resultCode shouldBe searchCode
              resultDesc shouldBe searchDesc
          }
        }
    }

    case class ST(query: String, numMin: Int, topHit: (String, String), expectedDesc: Seq[String])

    Seq(
      ST("Dairy farming", 1, ("01410003", "Dairy farming"), Seq("farming")),
      ST("Withy", 1, ("02200015", "Withy growing"), Seq("Withy")),
      ST("Chilli growing", 1, ("01280006", "Chilli growing"), Seq("Chilli", "growing")),
      ST("IT Library", 3, ("91011010", "Library access to IT facilities including internet"), Seq()),
      ST("Cress", 1, ("01130008", "Cress growing"), Seq()),
      ST("Dry", 3, ("96010005", "Dry cleaner"), Seq()) // This one needs a bit more work
    ) foreach { data =>
      s"""should return at least ${data.numMin} result when searching for "${data.query}"  with a top hit of ${data.topHit}""" in {
        withSearcher { searcher =>
          val qp = new QueryParser(FIELD_DESC, analyzer)

          val result = searcher.search(qp.parse(data.query), 5)

          result.totalHits.toInt should be >= data.numMin

          val results = result.scoreDocs.toSeq map {
            result =>
              val doc = searcher.doc(result.doc)
              val code = doc.get(FIELD_CODE8)
              val description = doc.get(FIELD_DESC)
              (code, description)
          }

          results(0) shouldBe data.topHit

          // TODO look at improving these checks to assert the results are expected
          results map {
            case (_, description) =>
              if (data.expectedDesc.length > 0) {
                data.expectedDesc.filter(desc => description.contains(desc)).length should be >= 1
              }
          }
        }
      }
    }

    Seq(
      "Dairy farming",
      "growing",
      "Car"
    ) foreach { queryString =>
      s"""should return page 2 & 3 results when searching for "${queryString}" """ in {
        withSearcher { searcher =>
          val hitsPerPage = 5
          val p2StartIndex = hitsPerPage
          val p3StartIndex = 2 * hitsPerPage

          val qp = new QueryParser(FIELD_DESC, analyzer)
          val query = qp.parse(queryString)

          // search for the first page with an extra item (i.e. first of page 2)
          val resultPage1Plus1 = searcher.search(query, (2 * hitsPerPage) + 1)

          val initialHits = resultPage1Plus1.totalHits
          val p2ExpectedDoc = searcher.doc(resultPage1Plus1.scoreDocs(hitsPerPage).doc)
          val p2ExpectedResult = (p2ExpectedDoc.get(FIELD_CODE8), p2ExpectedDoc.get(FIELD_DESC))
          val p3ExpectedDoc = searcher.doc(resultPage1Plus1.scoreDocs(2 * hitsPerPage).doc)
          val p3ExpectedResult = (p3ExpectedDoc.get(FIELD_CODE8), p3ExpectedDoc.get(FIELD_DESC))

          // search for the 2nd page using a collector
          val p2Collector = TopScoreDocCollector.create(1000) // consider whether 1000 is too high
          searcher.search(query, p2Collector)
          val page2SearchResult = p2Collector.topDocs(p2StartIndex, hitsPerPage)
          val page2TopDoc = searcher.doc(page2SearchResult.scoreDocs(0).doc)
          val page2Result = (page2TopDoc.get(FIELD_CODE8), page2TopDoc.get(FIELD_DESC))

          page2SearchResult.totalHits shouldBe initialHits
          page2Result shouldBe p2ExpectedResult

          // search for the 3rd page using a collector
          val p3Collector = TopScoreDocCollector.create(1000) // consider whether 1000 is too high
          searcher.search(query, p3Collector)
          val page3SearchResult = p3Collector.topDocs(p3StartIndex, hitsPerPage)
          val page3TopDoc = searcher.doc(page3SearchResult.scoreDocs(0).doc)
          val page3Result = (page3TopDoc.get(FIELD_CODE8), page3TopDoc.get(FIELD_DESC))

          page3SearchResult.totalHits shouldBe initialHits
          page3Result shouldBe p3ExpectedResult
        }
      }
    }
  }
}
