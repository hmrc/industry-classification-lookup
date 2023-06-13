/*
 * Copyright 2023 HM Revenue & Customs
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
import org.apache.lucene.facet.{DrillDownQuery, DrillSideways, FacetsConfig}
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc, TopScoreDocCollector}
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.QueryBuilder
import play.api.mvc.Request
import services.QueryType.{QUERY_BOOSTER, QUERY_BUILDER, QUERY_PARSER}
import services.{FacetResults, SearchResult}
import utils.LoggingUtil

import java.nio.file.FileSystems
import scala.collection.JavaConverters._

trait IndexConnector extends LoggingUtil {

  val name: String

  val LANG_EN = "en"
  val LANG_CY = "cy"

  val FIELD_CODE = "code"
  val FIELD_DESC = "description"
  val FIELD_DESC_CY = "descriptionCy"
  val FIELD_SEARCH_TERMS = "searchTerms"
  val FIELD_SEARCH_TERMS_CY = "searchTermsCy"
  val FIELD_SECTOR = "sector"

  val config: ICLConfig

  val indexLocation: String = config.getConfigString("index.path")

  val analyzer: String => StandardAnalyzer = (lang: String) => {
    val STOP_WORDS = List(
      "a", "an", "and", "are", "as", "at", "be", "but", "by",
      "for", "if", "in", "into", "is", // "it",
      "no", "not", "of", "on", "or", "such",
      "that", "the", "their", "then", "there", "these",
      "they", "this", "to", "was", "will", "with"
    )
    val STOP_WORDS_CY = List()
    val stopWords = lang match {
      case LANG_EN => STOP_WORDS.asJava
      case LANG_CY => STOP_WORDS_CY.asJava
    }
    new StandardAnalyzer(new CharArraySet(stopWords, true))
  }

  def index(indexName: String): NIOFSDirectory = {
    val path = FileSystems.getDefault.getPath(indexLocation, indexName)
    new NIOFSDirectory(path)
  }

  lazy val reader: DirectoryReader = DirectoryReader.open(index(name))
  lazy val searcher = new IndexSearcher(reader)

  private def extractSic(result: ScoreDoc) = {
    val doc = searcher.doc(result.doc)
    SicCode(doc.get(FIELD_CODE), doc.get(FIELD_DESC), doc.get(FIELD_DESC_CY))
  }

  def lookup(sicCode: String)(implicit request: Request[_]): Option[SicCode] = {
    val qp = new QueryParser(FIELD_CODE, analyzer(LANG_EN))

    val results = searcher.search(qp.parse(sicCode), 1)

    results.totalHits match {
      case 0 =>
        infoLog(s"[IndexConnector][lookup] Search for SIC code $sicCode found nothing")
        None
      case _ => Some(extractSic(results.scoreDocs(0)))
    }
  }

  def search(query: String,
             pageResults: Int = 5,
             page: Int = 1,
             sector: Option[String] = None,
             queryType: Option[String] = None,
             isFuzzyExecuted: Boolean = false,
             lang: String = LANG_EN)(implicit request: Request[_]): SearchResult = {

    val parsedQuery = buildQuery(query, queryType, isFuzzyExecuted, lang)

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
          infoLog(s"""Search for SIC codes with query "$query" found nothing performing fuzzy search""")
          search(query, pageResults, page, sector, queryType, isFuzzyExecuted = true)
        } else {
          infoLog(s"""Search for SIC codes with query "$query" found nothing""")
          SearchResult(0, 0, Seq(), Seq())
        }
      case n =>
        infoLog(s"""Search for SIC codes with query "$query" found $n results""")
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

  private[connectors] def buildQuery(query: String, queryType: Option[String] = None, isFuzzySearchNeeded: Boolean, lang: String): Query = {
    val searchField = if(lang == LANG_CY) FIELD_SEARCH_TERMS_CY else FIELD_SEARCH_TERMS
    if (isFuzzySearchNeeded) {
      FuzzyMatch(searchField, query, analyzer(lang), queryType.contains(QUERY_BOOSTER))
    } else {
      queryType match {
        case Some(QUERY_BUILDER) => new QueryBuilder(analyzer(lang)).createBooleanQuery(searchField, query)
        case Some(QUERY_PARSER) => new QueryParser(searchField, analyzer(lang)).parse(query)
        case Some(QUERY_BOOSTER) => QueryBooster(searchField, query, 5)
        case _ => throw new RuntimeException("No queryType provided")
      }
    }
  }

  private[connectors] def getSectorName(sectorCode: String, isWelsh: Boolean = false): String = {
    if(isWelsh) {config.getConfigString(s"sic.sectorCy.${sectorCode.toUpperCase}")}
    else {config.getConfigString(s"sic.sector.${sectorCode.toUpperCase}")}
  }
}

