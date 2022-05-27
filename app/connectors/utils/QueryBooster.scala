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

package connectors.utils

import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.{BooleanQuery, BoostQuery, Query, TermQuery}

object QueryBooster {
  def apply(fieldName: String, query: String, boostFactor: Float): Query = {
    val splitSearchParams = query.toLowerCase.split(" ")
    val queryBuilder = new BooleanQuery.Builder()

    queryBuilder.add(new BoostQuery(new TermQuery(new Term(fieldName, splitSearchParams.head)), boostFactor), Occur.SHOULD)
    splitSearchParams.tail.foreach(value => queryBuilder.add(new TermQuery(new Term(fieldName, value)), Occur.SHOULD))
    queryBuilder.build()
  }
}

