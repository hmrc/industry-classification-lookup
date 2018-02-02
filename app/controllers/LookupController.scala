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

package controllers

import javax.inject.{Inject, Singleton}

import config.{MicroserviceAuthConnector, MicroserviceConfig}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.LookupService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class LookupControllerImpl @Inject()(val lookupService: LookupService,
                                     val config: MicroserviceConfig,
                                     val authConnector: MicroserviceAuthConnector) extends LookupController {
  val defaultIndex = config.getConfigString("index.default")

}

trait LookupController extends BaseController with AuthorisedFunctions {

  val lookupService: LookupService
  val defaultIndex: String

  def lookup(sicCode: String, indexName: Option[String]): Action[AnyContent] = Action.async{
    implicit request =>
      authorised() {
        val idxName = indexName.getOrElse(defaultIndex)
        lookupService.lookup(sicCode, idxName) match {
          case Some(sic) => Future.successful(Ok(Json.toJson(sic)))
          case None => Future.successful(NotFound)
        }
      }
  }
}
