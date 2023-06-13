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

package services

import connectors.{GDSRegisterSIC5IndexConnector, ONSSupplementSIC5IndexConnector}
import models.SicCode
import play.api.libs.json.{Format, Json}
import play.api.mvc.Request

import javax.inject.{Inject, Singleton}

object Indexes {
  final val GDS_REGISTER_SIC5_INDEX = "gds-register-sic5"
  final val ONS_SUPPLEMENT_SIC5_INDEX = "ons-supplement-sic5"
}

import services.Indexes._

@Singleton
class LookupService @Inject()(gdsIndex: GDSRegisterSIC5IndexConnector,
                              onsIndex: ONSSupplementSIC5IndexConnector) {
  val indexes = Map(
    GDS_REGISTER_SIC5_INDEX -> gdsIndex,
    ONS_SUPPLEMENT_SIC5_INDEX -> onsIndex
  )


  def lookup(sicCodes: List[String])(implicit request: Request[_]): List[SicCode] = {
    sicCodes flatMap (code => indexes(GDS_REGISTER_SIC5_INDEX).lookup(code))
  }

  def search(query: String,
             indexName: String,
             pageResults: Option[Int] = None,
             page: Option[Int] = None,
             sector: Option[String] = None,
             queryParser: Option[Boolean] = None,
             queryBoostFirstTerm: Option[Boolean] = None,
             lang: String)(implicit request: Request[_]): SearchResult = {
    val queryType: Option[String] = (queryParser, queryBoostFirstTerm) match {
      case (Some(true), _) => Some("query-parser")
      case (_, Some(true)) => Some("query-boost-first-term")
      case _ => Some("query-builder")
    }

    indexes(indexName).search(query, pageResults.getOrElse(5), page.getOrElse(1), sector, queryType, lang = lang)
  }
}

case class FacetResults(code: String, name: String, nameCy: String, count: Int)

object FacetResults {
  implicit val formats: Format[FacetResults] = Json.format[FacetResults]
}

case class SearchResult(numFound: Long, nonFilteredFound: Long = 0, results: Seq[SicCode], sectors: Seq[FacetResults])

object SearchResult {
  implicit val formats: Format[SearchResult] = Json.format[SearchResult]
}

object QueryType {
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER = "query-parser"
  val QUERY_BOOSTER = "query-boost-first-term"
}
