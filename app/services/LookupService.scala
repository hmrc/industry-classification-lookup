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

import java.nio.file.{FileSystems, Path}
import javax.inject.{Inject, Singleton}

import config.MicroserviceConfig
import models.SicCode
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.{DrillDownQuery, DrillSideways, FacetsCollector, FacetsConfig}
import org.apache.lucene.facet.sortedset.{DefaultSortedSetDocValuesReaderState, SortedSetDocValuesFacetCounts}
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc, TopScoreDocCollector}
import org.apache.lucene.store.NIOFSDirectory
import play.api.Logger
import play.api.libs.json.{Format, Json}

@Singleton
class LookupServiceImpl @Inject()(val config: MicroserviceConfig,
                                  val sic8Index: SIC8IndexConnector) extends LookupService

trait LookupService {

  val config: MicroserviceConfig
  val sic8Index: SIC8IndexConnector

  def lookup(sicCode: String): Option[SicCode] = {
    sic8Index.lookup(sicCode)
  }

  def search(query: String, pageResults: Option[Int] = None, page: Option[Int] = None, sector: Option[String] = None): SearchResult = {
    sic8Index.search(query, pageResults.getOrElse(5), page.getOrElse(1), sector)
  }
}

case class FacetResults(code: String, name: String, count: Int)
object FacetResults { implicit val formats: Format[FacetResults] = Json.format[FacetResults] }
case class SearchResult(numFound: Long, nonFilteredFound: Long = 0, results: Seq[SicCode], sectors: Seq[FacetResults])
object SearchResult { implicit val formats: Format[SearchResult] = Json.format[SearchResult] }

@Singleton
class SIC8IndexConnectorImpl @Inject()(val config: MicroserviceConfig) extends SIC8IndexConnector

trait SIC8IndexConnector {

  val FIELD_CODE8 = "code8"
  val FIELD_DESC = "description"
  val FIELD_SECTOR = "sector"

  val config: MicroserviceConfig

  val analyzer: StandardAnalyzer = {
    import scala.collection.JavaConverters._
    val stopWords = List(
      "a", "an", "and", "are", "as", "at", "be", "but", "by",
      "for", "if", "in", "into", "is", // "it",
      "no", "not", "of", "on", "or", "such",
      "that", "the", "their", "then", "there", "these",
      "they", "this", "to", "was", "will", "with"
    )

    new StandardAnalyzer(new CharArraySet(stopWords.asJava, true))
  }

  // TODO - when should we close the index?
  val index: NIOFSDirectory = {
    val path: Path = FileSystems.getDefault.getPath("conf", "index")
    new NIOFSDirectory(path)
  }

  val reader: DirectoryReader = DirectoryReader.open(index)
  val searcher = new IndexSearcher(reader)
  val facetsCollector = new FacetsCollector()

  private def extractSic(result: ScoreDoc) = {
    val doc = searcher.doc(result.doc)
    SicCode(doc.get(FIELD_CODE8), doc.get(FIELD_DESC))
  }

  def lookup(sicCode: String): Option[SicCode] = {
    val qp = new QueryParser(FIELD_CODE8, analyzer) // TODO QueryBuilder?

    val results = searcher.search(qp.parse(sicCode), 1)

    results.totalHits match {
      case 0 =>
        Logger.info(s"Search for SIC code $sicCode found nothing")
        None
      case _ => Some(extractSic(results.scoreDocs(0)))
    }
  }

  def search(query: String, pageResults: Int = 5, page: Int = 1, sector: Option[String] = None): SearchResult = {

    val queryParser = new QueryParser(FIELD_DESC, analyzer) // TODO QueryBuilder?
    val parsedQuery = queryParser.parse(query)
    val collector: TopScoreDocCollector = TopScoreDocCollector.create(1000)

    val facetConfig = new FacetsConfig

    val drillDownQuery = new DrillDownQuery(facetConfig, parsedQuery)

    sector foreach (s => drillDownQuery.add(FIELD_SECTOR, s))

    val readerState = new DefaultSortedSetDocValuesReaderState(reader)
    val drillSideways = new DrillSideways(searcher, facetConfig, readerState)

    val result = drillSideways.search(drillDownQuery, collector)

    val startIndex = if(page > 0) (page - 1) * pageResults else 0
    val results = collector.topDocs(startIndex, pageResults)

    results.totalHits match {
      case 0 =>
        Logger.info(s"""Search for SIC codes with query "$query" found nothing""")
        SearchResult(0, 0, Seq(), Seq())
      case n =>
        Logger.info(s"""Search for SIC codes with query "$query" found $n results""")
        val sics = results.scoreDocs.toSeq map extractSic
        val facetResults: Seq[FacetResults] = {
          result.facets.getTopChildren(1000, FIELD_SECTOR).labelValues.toSeq map { lv =>
            FacetResults(lv.label, getSectorName(lv.label), lv.value.intValue())
          }
        }
        val nonFilteredCount = facetResults.map(_.count).sum
        SearchResult(n, nonFilteredCount, sics, facetResults)
    }
  }

  private[services] def getSectorName(sectorCode: String): String = {
    config.getConfigString(s"sic.sector.${sectorCode.toUpperCase}")
  }
}
