/*
 * Copyright 2024 HM Revenue & Customs
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

trait SICSearchHelper extends IntegrationSpecBase {

  def buildQuery(query: String,
                 indexName: String,
                 maxResults: Option[Int] = None,
                 page: Option[Int] = None,
                 sector: Option[String] = None,
                 queryParser: Option[Boolean] = None,
                 queryBoostFirstTerm: Option[Boolean] = None,
                 lang: String = "en") = {
    val maxParam = maxResults.fold("")(n => s"&pageResults=$n")
    val indexNameParam = s"&indexName=$indexName"
    val pageParam = page.fold("")(n => s"&page=$n")
    val sectorParam = sector.fold("")(s => s"&sector=$s")
    val queryParserParam = queryParser.fold("")(s => s"&queryParser=$s")
    val queryBoostFirstTermParam = queryBoostFirstTerm.fold("")(s => s"&queryBoostFirstTerm=$s")
    buildClient(s"/search?query=$query$indexNameParam$maxParam$pageParam$sectorParam$queryParserParam$queryBoostFirstTermParam&lang=$lang")
  }

}
