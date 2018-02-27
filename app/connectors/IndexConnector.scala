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

package connectors

import java.nio.file.FileSystems
import javax.inject.Inject

import config.ICLConfig
import models.SicCode
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.{DrillDownQuery, DrillSideways, FacetsCollector, FacetsConfig}
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc, TopScoreDocCollector}
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.QueryBuilder
import play.api.Logger
import services.{FacetResults, SearchResult}
import services.Indexes._

class GDSRegisterSIC5IndexConnectorImpl @Inject()(val config: ICLConfig) extends IndexConnector {
  override val name = GDS_REGISTER_SIC5_INDEX
}

class ONSSupplementSIC5IndexConnectorImpl @Inject()(val config: ICLConfig) extends IndexConnector {
  override val name = ONS_SUPPLEMENT_SIC5_INDEX
}

class SIC8IndexConnectorImpl @Inject()(val config: ICLConfig) extends IndexConnector {
  override val name = HMRC_SIC8_INDEX
}

trait IndexConnector {

  val name: String
  val FIELD_CODE = "code"
  val FIELD_DESC = "description"
  val FIELD_SEARCH_TERMS = "searchTerms"
  val FIELD_SECTOR = "sector"

  val config: ICLConfig

  val indexLocation = config.getConfigString("index.path")

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

  def index(indexName: String): NIOFSDirectory = {
    val path = FileSystems.getDefault().getPath(indexLocation, indexName)
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
        Logger.info(s"Search for SIC code $sicCode found nothing")
        None
      case _ => Some(extractSic(results.scoreDocs(0)))
    }
  }

  def search(query: String,
             pageResults: Int = 5,
             page: Int = 1,
             sector: Option[String] = None,
             queryType: Option[String] = None): SearchResult = {

    val parsedQuery = buildQuery(query, queryType)

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

  private[connectors] def buildQuery(query: String, queryType: Option[String] = None): Query = {
    import services.QueryType._
    queryType match {
      case Some(QUERY_BUILDER) => new QueryBuilder(analyzer).createBooleanQuery(FIELD_SEARCH_TERMS, query)
      case Some(QUERY_PARSER)  => new QueryParser(FIELD_SEARCH_TERMS, analyzer).parse(query)
      case _                   => throw new RuntimeException("No queryType provided")
    }
  }

  private[connectors] def getSectorName(sectorCode: String): String = {
    config.getConfigString(s"sic.sector.${sectorCode.toUpperCase}")
  }
}