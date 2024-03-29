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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.LookupService
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LookupController @Inject()(lookupService: LookupService,
                                 val authConnector: AuthConnector,
                                 controllerComponents: ControllerComponents)
                                (implicit executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with AuthorisedFunctions with LoggingUtil {

  def lookup(sicCodes: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      lookupService.lookup(sicCodes.split(",").toList) match {
        case Nil =>
          infoLog("[LookupController][lookup] No SIC codes found")
          Future.successful(NoContent)
        case list =>
          infoLog("[LookupController][lookup] SIC codes found")
          Future.successful(Ok(Json.toJson(list.distinct)))
      }
    }
  }
}
