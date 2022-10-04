/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.utils

import config.ICLConfig
import models.SicCode
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState
import org.apache.lucene.facet.{DrillDownQuery, DrillSideways, FacetsCollector, FacetsConfig}
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc, TopScoreDocCollector}
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.QueryBuilder
import play.api.Logging
import services.QueryType.{QUERY_BOOSTER, QUERY_BUILDER, QUERY_PARSER}
import services.{FacetResults, SearchResult}

import java.nio.file.FileSystems
import scala.collection.JavaConverters._

trait IndexConnector extends Logging {

  val name: String
  val FIELD_CODE = "code"
  val FIELD_DESC = "description"
  val FIELD_SEARCH_TERMS = "searchTerms"
  val FIELD_SECTOR = "sector"

  val config: ICLConfig

  val indexLocation = config.getConfigString("index.path")

  val analyzer: StandardAnalyzer = {
    val stopWords = List(
      "a", "an", "and", "are", "as", "at", "be", "but", "by",
      "for", "if", "in", "into", "is", // "it",
      "no", "not", "of", "on", "or", "such",
      "that", "the", "their", "then", "there", "these",
      "they", "this", "to", "was", "will", "with"
    )

    new StandardAnalyzer(new CharArraySet(stopWords.asJava, true))
  }

  def index(indexName: String): NIOFSDirectory = {
    val path = FileSystems.getDefault.getPath(indexLocation, indexName)
    new NIOFSDirectory(path)
  }

  lazy val reader: DirectoryReader = DirectoryReader.open(index(name))
  lazy val searcher = new IndexSearcher(reader)
  val facetsCollector = new FacetsCollector()

  private def extractSic(result: ScoreDoc) = {
    val doc = searcher.doc(result.doc)
    SicCode(doc.get(FIELD_CODE), doc.get(FIELD_DESC))
  }

  def lookup(sicCode: String): Option[SicCode] = {
    val qp = new QueryParser(FIELD_CODE, analyzer)

    val results = searcher.search(qp.parse(sicCode), 1)

    results.totalHits match {
      case 0 =>
        logger.info(s"Search for SIC code $sicCode found nothing")
        None
      case _ => Some(extractSic(results.scoreDocs(0)))
    }
  }

  def search(query: String,
             pageResults: Int = 5,
             page: Int = 1,
             sector: Option[String] = None,
             queryType: Option[String] = None,
             isFuzzyExecuted: Boolean = false): SearchResult = {

    val parsedQuery = buildQuery(query, queryType, isFuzzyExecuted)

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
        if(!queryType.contains(QUERY_PARSER) && !isFuzzyExecuted) {
          logger.info(s"""Search for SIC codes with query "$query" found nothing performing fuzzy search""")
          search(query, pageResults, page, sector, queryType, true)
        } else {
          logger.info(s"""Search for SIC codes with query "$query" found nothing""")
          SearchResult(0, 0, Seq(), Seq())
        }
      case n =>
        logger.info(s"""Search for SIC codes with query "$query" found $n results""")
        val sics = results.scoreDocs.toSeq map extractSic
        val facetResults: Seq[FacetResults] = {
          result.facets.getTopChildren(1000, FIELD_SECTOR).labelValues.toSeq map { lv =>
            FacetResults(lv.label, getSectorName(lv.label), getSectorName(lv.label, isWelsh = true), lv.value.intValue())
          }
        }
        val nonFilteredCount = facetResults.map(_.count).sum
        SearchResult(n, nonFilteredCount, sics, facetResults)
    }
  }

  private[connectors] def buildQuery(query: String, queryType: Option[String] = None, isFuzzySearchNeeded: Boolean): Query = {
    if (isFuzzySearchNeeded) {
      FuzzyMatch(FIELD_SEARCH_TERMS, query, analyzer, queryType.contains(QUERY_BOOSTER))
    } else {
      queryType match {
        case Some(QUERY_BUILDER) => new QueryBuilder(analyzer).createBooleanQuery(FIELD_SEARCH_TERMS, query)
        case Some(QUERY_PARSER) => new QueryParser(FIELD_SEARCH_TERMS, analyzer).parse(query)
        case Some(QUERY_BOOSTER) => QueryBooster(FIELD_SEARCH_TERMS, query, 5)
        case _ => throw new RuntimeException("No queryType provided")
      }
    }
  }

  private[connectors] def getSectorName(sectorCode: String, isWelsh: Boolean = false): String = {
    if(isWelsh) {config.getConfigString(s"sic.sectorCy.${sectorCode.toUpperCase}")}
    else {config.getConfigString(s"sic.sector.${sectorCode.toUpperCase}")}
  }
}

