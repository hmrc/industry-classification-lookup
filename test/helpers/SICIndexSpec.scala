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

package helpers

import java.nio.file.FileSystems

import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState
import org.apache.lucene.index.{DirectoryReader, IndexReader}
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, NIOFSDirectory}
import org.scalatestplus.play.PlaySpec

trait SICIndexSpec extends PlaySpec {

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

  def openIndex() = new NIOFSDirectory(indexPath)

  def withSearcher(f: IndexSearcher => Unit): Unit = {
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

  def setupFacetedSearch() = {
    val reader: IndexReader = DirectoryReader.open(openIndex())

    val searcher = new IndexSearcher(reader)
    val state = new DefaultSortedSetDocValuesReaderState(reader)

    (searcher, state)
  }

  case class ST(query: String, numMin: Int, topHit: (String, String), expectedDesc: Seq[String])

}
