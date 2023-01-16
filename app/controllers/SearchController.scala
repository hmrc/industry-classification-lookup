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

package controllers

import config.ICLConfig
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.LookupService
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchController @Inject()(lookupService: LookupService,
                                 config: ICLConfig,
                                 val authConnector: AuthConnector,
                                 controllerComponents: ControllerComponents)
                                (implicit executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with AuthorisedFunctions {

  val defaultIndex = config.getConfigString("index.default")

  def search(query: String,
             indexName: Option[String],
             pageResults: Option[Int] = None,
             page: Option[Int],
             sector: Option[String] = None,
             queryParser: Option[Boolean] = None,
             queryBoostFirstTerm: Option[Boolean] = None,
             lang: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        val idxName = indexName.getOrElse(defaultIndex)
        val results = lookupService.search(query, idxName, pageResults, page, sector, queryParser, queryBoostFirstTerm, lang = lang)
        Future.successful(Ok(Json.toJson(results)))
      }
  }
}
