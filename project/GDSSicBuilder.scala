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

// Build the index for the GDS Register five digit variant constructed from the 2003 & 2007 lists
object GDSSicBuilder extends SICIndexBuilder {

  val name = "gds-register-sic5"

  def produceDocuments(addDocument: GDSSicBuilder.AddDocument) = {
    import scala.io.Source
    var docsAdded = 0
    val fileSicPipe = "index-src/GDSRegisterDataSet.txt"
    val source = Source.fromFile(fileSicPipe)
    for (line <- source.getLines()) {
      val split = line.split("\t")
      val code = split(0)
      val desc = split(1)
      addDocument(SicDocument(code, desc, desc))
      docsAdded += 1
    }
    source.close()

    docsAdded
  }
}

