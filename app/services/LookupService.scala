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

package services

import javax.inject.{Inject, Named, Singleton}

import config.ICLConfig
import connectors.IndexConnector
import models.SicCode
import play.api.libs.json.{Format, Json}

object Indexes {
  final val HMRC_SIC8_INDEX = "hmrc-sic8"
  final val GDS_REGISTER_SIC5_INDEX = "gds-register-sic5"
  final val ONS_SUPPLEMENT_SIC5_INDEX = "ons-supplement-sic5"
}

import services.Indexes._

class LookupServiceImpl @Inject()(val config: ICLConfig,
                                  @Named(HMRC_SIC8_INDEX) val hmrcSic8Index: IndexConnector,
                                  @Named(GDS_REGISTER_SIC5_INDEX) val gdsIndex: IndexConnector,
                                  @Named(ONS_SUPPLEMENT_SIC5_INDEX) val onsIndex: IndexConnector
                                 ) extends LookupService {
  val indexes = Map(
    HMRC_SIC8_INDEX           -> hmrcSic8Index,
    GDS_REGISTER_SIC5_INDEX   -> gdsIndex,
    ONS_SUPPLEMENT_SIC5_INDEX -> onsIndex
  )
}

trait LookupService {

  val config: ICLConfig
  val indexes: Map[String, IndexConnector]

  def lookup(sicCode: String, indexName: String): Option[SicCode] = {
    indexes(indexName).lookup(sicCode)
  }

  def search(query: String,
             indexName: String,
             pageResults: Option[Int] = None,
             page: Option[Int] = None,
             sector: Option[String] = None,
             queryType: Option[String] = None): SearchResult = {
    indexes(indexName).search(query, pageResults.getOrElse(5), page.getOrElse(1), sector, queryType)
  }
}

case class FacetResults(code: String, name: String, count: Int)
object FacetResults { implicit val formats: Format[FacetResults] = Json.format[FacetResults] }

case class SearchResult(numFound: Long, nonFilteredFound: Long = 0, results: Seq[SicCode], sectors: Seq[FacetResults])
object SearchResult { implicit val formats: Format[SearchResult] = Json.format[SearchResult] }

object QueryType {
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER  = "query-parser"
  val QUERY_BOOSTER = "query-boost-first-term"
}
