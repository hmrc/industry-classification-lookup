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

package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.LookupService
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class LookupController @Inject()(lookupService: LookupService,
                                 val authConnector: AuthConnector,
                                 controllerComponents: ControllerComponents)
  extends BackendController(controllerComponents) with AuthorisedFunctions {

  def lookup(sicCodes: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      lookupService.lookup(sicCodes.split(",").toList) match {
        case Nil => Future.successful(NoContent)
        case list => Future.successful(Ok(Json.toJson(list.distinct)))
      }
    }
  }
}
