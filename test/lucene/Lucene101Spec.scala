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

package lucene

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.QueryBuilder
import uk.gov.hmrc.play.test.UnitSpec


class Lucene101Spec extends UnitSpec {

  "Wibble" should {

    val analyzer = new StandardAnalyzer();

    def buildIndex() = {
      val config = new IndexWriterConfig(analyzer);
      def addDoc(w: IndexWriter, title: String, isbn: String) {
        val doc = new Document
        doc.add(new TextField("title", title, Field.Store.YES))
        doc.add(new StringField("isbn", isbn, Field.Store.YES))
        w.addDocument(doc)
      }

      val index: Directory = new RAMDirectory();

      val w = new IndexWriter(index, config);
      addDoc(w, "Lucene in Action", "193398817");
      addDoc(w, "Lucene for Dummies", "55320055Z");
      addDoc(w, "Managing Gigabytes", "55063554A");
      addDoc(w, "The Art of Computer Science", "9900333X");
      w.close();

      index
    }

    "foo" in {
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)

      val qp = new QueryParser("title", analyzer)

      val result = searcher.search(qp.parse("Lucene^2 OR Science"), 5)

      result.totalHits shouldBe 3

      result.scoreDocs.toSeq map {
        result =>
          val doc = searcher.doc(result.doc)
          val title = doc.get("title")
          val isbn = doc.get("isbn") //getFields("isbn").head.stringValue()
          (isbn, title)
      } shouldBe Seq(("193398817", "Lucene in Action"), ("55320055Z", "Lucene for Dummies"), ("9900333X", "The Art of Computer Science"))
    }

    "foo2" in {
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)

      val builder = new QueryBuilder(analyzer)
      val query = builder.createBooleanQuery("title", "Lucene")

      //     val query = new TermQuery(new Term("title", "Lucene"))

      //     val query: Query = new BooleanQuery.Builder().
      //       add(new TermQuery(new Term("title", "Lucene")), BooleanClause.Occur.MUST).
      //       add(new WildcardQuery(new Term("title", "Lucene")), BooleanClause.Occur.MUST).
      //       add(new PrefixQuery(new Term("title", "Lucene")), BooleanClause.Occur.MUST).
      //       build()

      val result = searcher.search(query, 5)

      result.totalHits shouldBe 2

      result.scoreDocs.toSeq map {
        result =>
          val doc = searcher.doc(result.doc)
          val title = doc.get("title")
          //getFields("title").head.stringValue()
          val isbn = doc.get("isbn") //getFields("isbn").head.stringValue()
          (isbn, title)
      } shouldBe Seq(("193398817", "Lucene in Action"), ("55320055Z", "Lucene for Dummies"))
    }
  }
}
