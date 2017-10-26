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

package config

import com.google.inject.AbstractModule
import controllers._
import services.{SIC8IndexConnector, SIC8IndexConnectorImpl, LookupService, LookupServiceImpl}
import uk.gov.hmrc.play.config.inject.{DefaultServicesConfig, ServicesConfig}

class Module extends AbstractModule {

  override def configure() {
    bindControllers()
    bindServices()
    bindConnecors()
    bindConfig()
  }

  private def bindControllers() {
    bind(classOf[LookupController]).to(classOf[LookupControllerImpl])
    bind(classOf[SearchController]).to(classOf[SearchControllerImpl])
  }

  private def bindServices() {
    bind(classOf[LookupService]).to(classOf[LookupServiceImpl])
  }

  private def bindConnecors() {
    bind(classOf[SIC8IndexConnector]).to(classOf[SIC8IndexConnectorImpl])
  }

  private def bindConfig() {
    bind(classOf[ServicesConfig]).to(classOf[DefaultServicesConfig])
    bind(classOf[MicroserviceConfig]).to(classOf[MicroserviceConfigImpl])
  }
}
