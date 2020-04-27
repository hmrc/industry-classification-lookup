/*
 * Copyright 2020 HM Revenue & Customs
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
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.QueryBuilder
import org.scalatestplus.play.PlaySpec


class Lucene101Spec extends PlaySpec {

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

      result.totalHits mustBe 3

      result.scoreDocs.toSeq map {
        result =>
          val doc = searcher.doc(result.doc)
          val title = doc.get("title")
          val isbn = doc.get("isbn") //getFields("isbn").head.stringValue()
          (isbn, title)
      } mustBe Seq(("193398817", "Lucene in Action"), ("55320055Z", "Lucene for Dummies"), ("9900333X", "The Art of Computer Science"))
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

      result.totalHits mustBe 2

      result.scoreDocs.toSeq map {
        result =>
          val doc = searcher.doc(result.doc)
          val title = doc.get("title")
          //getFields("title").head.stringValue()
          val isbn = doc.get("isbn") //getFields("isbn").head.stringValue()
          (isbn, title)
      } mustBe Seq(("193398817", "Lucene in Action"), ("55320055Z", "Lucene for Dummies"))
    }

    "boost the first search term" in {
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)

      val query = new BooleanQuery.Builder()
      query.add(new BoostQuery(new TermQuery(new Term("title", "dummies")), 2), Occur.SHOULD)
      query.add(new TermQuery(new Term("title", "science")), Occur.SHOULD).build()

      val result = searcher.search(query.build(), 5)

      result.totalHits mustBe 2

      result.scoreDocs.map { res =>
        val doc = searcher.doc(res.doc)
        doc.get("title")
      }.toSeq mustBe Seq("Lucene for Dummies", "The Art of Computer Science")
    }

    "boost the first search term (each parameter added separately)" in {
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)

      val searchParameter = "dummies managing computer"
      val splitSearchParams = searchParameter.split(" ")

      val query = new BooleanQuery.Builder()
      query.add(new BoostQuery(new TermQuery(new Term("title", splitSearchParams(0))), 2), Occur.SHOULD)
      splitSearchParams.tail.foreach(value => query.add(new TermQuery(new Term("title", value)), Occur.SHOULD))


      val result = searcher.search(query.build(), 5)

      result.totalHits mustBe 3

      result.scoreDocs.map { res =>
        val doc = searcher.doc(res.doc)
        doc.get("title")
      }.toSeq mustBe Seq("Lucene for Dummies", "Managing Gigabytes", "The Art of Computer Science")
    }

    "boost the first search term (first param in own boost and the rest in same TermQuery)" in {
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)

      val searchParameter = "dummies managing computer"
      val splitSearchParams = searchParameter.split(" ")

      val query = new BooleanQuery.Builder()
      query.add(new BoostQuery(new TermQuery(new Term("title", splitSearchParams(0))), 2), Occur.SHOULD)
      query.add(new TermQuery(new Term("title", splitSearchParams.tail.mkString(" "))), Occur.SHOULD)


      val result = searcher.search(query.build(), 5)

      result.totalHits mustBe 1

      result.scoreDocs.map { res =>
        val doc = searcher.doc(res.doc)
        doc.get("title")
      }.toSeq mustBe Seq("Lucene for Dummies")
    }

    "fuzzy search and boost first term" in {
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)

      val searchParameter = "scence lucne gigbytes"
      val splitSearchParams = searchParameter.split(" ")

      val query = new BooleanQuery.Builder()
      query.add(new BoostQuery(new FuzzyQuery(new Term("title", splitSearchParams(0))), 2), Occur.SHOULD)
      splitSearchParams.tail.foreach(value => query.add(new FuzzyQuery(new Term("title", value)), Occur.SHOULD))

      val result = searcher.search(query.build(), 5)

      result.totalHits mustBe 4

      result.scoreDocs.map { res =>
        val doc = searcher.doc(res.doc)
        doc.get("title")
      }.toSeq mustBe Seq("The Art of Computer Science", "Managing Gigabytes", "Lucene in Action", "Lucene for Dummies")
    }

    "fuzzzzzzzy" should {
      "kick in if the initial search returned no results (dumbies)" in {
        val index = buildIndex()

        val reader: IndexReader = DirectoryReader.open(index)
        val searcher = new IndexSearcher(reader)

        object FuzzyMatch {
          def apply(fieldName: String, query: String): Query = {
            new QueryBuilder(analyzer).createBooleanQuery(fieldName, query)
          }

          def apply(fieldName: String, query: String, isFuzzy: Boolean): Query = {
            val splitSearchParams = query.toLowerCase.split(" ")
            new FuzzyQuery(new Term(fieldName, query))
          }
        }

        val query = FuzzyMatch("title", "dumbies")

        val result = searcher.search(query, 5)

        result.totalHits mustBe 0

        val secondQuery = FuzzyMatch("title", "dumbies", isFuzzy = true)

        val resultTwo = searcher.search(secondQuery, 5)

        resultTwo.totalHits mustBe 1

        resultTwo.scoreDocs.map { res =>
          val doc = searcher.doc(res.doc)
          doc.get("title")
        }.toSeq mustBe Seq("Lucene for Dummies")
      }

      "kick in if the initial search returned no results (dumbies Gigggabytes)" in {
        val index = buildIndex()

        val reader: IndexReader = DirectoryReader.open(index)
        val searcher = new IndexSearcher(reader)

        object FuzzyMatch {
          private def normalSearch(fieldName: String, query: String): Query = {
            new QueryBuilder(analyzer).createBooleanQuery(fieldName, query)
          }

          private def fuzzySearch(fieldName: String, query: String): Query = {
            val splitSearchParams = query.toLowerCase.split(" ")
            val queryBuilder = new BooleanQuery.Builder()

            splitSearchParams.foreach(value => queryBuilder.add(new FuzzyQuery(new Term(fieldName, value)), Occur.SHOULD))
            queryBuilder.build()
          }

          def apply(fieldName: String, query: String, isFuzzy: Boolean = false): Query =
            if (isFuzzy) fuzzySearch(fieldName, query) else normalSearch(fieldName, query)
        }

        val query = FuzzyMatch("title", "dumbies Gigggabytes")

        val result = searcher.search(query, 5)

        result.totalHits mustBe 0

        val secondQuery = FuzzyMatch("title", "dumbies Gigggabytes", isFuzzy = true)

        val resultTwo = searcher.search(secondQuery, 5)

        resultTwo.totalHits mustBe 2

        resultTwo.scoreDocs.map { res =>
          val doc = searcher.doc(res.doc)
          doc.get("title")
        }.toSeq mustBe Seq("Lucene for Dummies", "Managing Gigabytes")
      }
    }
  }
}
