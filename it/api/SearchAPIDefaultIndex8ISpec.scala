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

package api

import helpers.IntegrationSpecBase
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSResponse
import services.Indexes.HMRC_SIC8_INDEX
import services.QueryType.QUERY_PARSER

class SearchAPIDefaultIndex8ISpec extends IntegrationSpecBase {

  "calling GET /search with no index should use the default" when {

    val query = "Dairy+farming"

    def buildQuery(query: String, indexName: Option[String] = None, queryType: Option[String] = Some(QUERY_PARSER)) = {
      val maxParam = "&pageResults=5"
      val indexNameParam = indexName.fold("")(n => s"&indexName=${n}")
      val queryTypeParam = queryType.fold("")(s => s"&queryType=$s")
      buildClient(s"/search?query=${query}${indexNameParam}${maxParam}${queryTypeParam}")
    }

    "supplying the query 'Dairy+farming' should return a 200 and the sic code descriptions as json" in {
      val clientDefault = buildQuery(query)
      val clientHMRC = buildQuery(query, Some(HMRC_SIC8_INDEX))

      setupSimpleAuthMocks()

      val responseDefault: WSResponse = clientDefault.get()
      val responseHMRC: WSResponse = clientHMRC.get()

      responseDefault.status shouldBe 200
      responseHMRC.status shouldBe 200
      responseDefault.json shouldBe responseHMRC.json
    }
  }
}
