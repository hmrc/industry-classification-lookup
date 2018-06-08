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
import org.apache.lucene.facet.sortedset.{DefaultSortedSetDocValuesReaderState, SortedSetDocValuesFacetCounts, SortedSetDocValuesFacetField}
import org.apache.lucene.facet.{DrillDownQuery, DrillSideways, FacetsCollector, FacetsConfig}
import org.apache.lucene.index._
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import uk.gov.hmrc.play.test.UnitSpec

class LuceneSearchesSpec extends UnitSpec {

  "LuceneSearchesSpec" should {

    val analyzer = new StandardAnalyzer();

    def buildIndex() = {
      val config            = new IndexWriterConfig(analyzer)
      val index: Directory  = new RAMDirectory()
      val indexWriter       = new IndexWriter(index, config)
      val facetConfig       = new FacetsConfig()

      def addDoc(title: String, isbn: String, sector: String) {
        val doc = new Document
        doc.add(new TextField("title", title, Field.Store.YES))
        doc.add(new StringField("isbn", isbn, Field.Store.YES))
        doc.add(new SortedSetDocValuesFacetField("sector", sector))
        indexWriter.addDocument(facetConfig.build(doc))
      }

      addDoc("Lucene in Action", "193398817", "Sector 1");
      addDoc("Lucene for Dummies", "55320055Z", "Sector 1");
      addDoc("Lucene for Dummies 2", "55320055Z", "Sector 2");
      addDoc("Managing Gigabytes", "55063554A", "Sector 2");
      addDoc("The Art of Computer Science", "9900333X", "Sector 3");

      indexWriter.close()

      index
    }

    def setupSearch() = {
      val facetConfig = new FacetsConfig()
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)

      val searcher = new IndexSearcher(reader)
      val state = new DefaultSortedSetDocValuesReaderState(reader)

      searcher
    }

    "Simple single term search" in {

      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val q = new TermQuery(new Term("title", "lucene"))

      searcher.search(q, tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 3

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("Lucene in Action", "Lucene for Dummies", "Lucene for Dummies 2")
    }

    "Simple single term search with limited results" in {
      val limit = 2
      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val q = new TermQuery(new Term("title", "lucene"))

      searcher.search(q, tdc)

      val results = tdc.topDocs(0, limit)
      results.scoreDocs.length shouldBe 2

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("Lucene in Action", "Lucene for Dummies")
    }

    "Simple multi-term AND search" in {

      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val bq = new BooleanQuery.Builder()
      bq.add(new TermQuery(new Term("title", "lucene")), Occur.MUST)
      bq.add(new TermQuery(new Term("title", "dummies")), Occur.MUST)

      searcher.search(bq.build(), tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 2

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("Lucene for Dummies", "Lucene for Dummies 2")
    }

    "Simple multi-term OR search" in {

      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val bq = new BooleanQuery.Builder()
      bq.add(new TermQuery(new Term("title", "dummies")), Occur.SHOULD)
      bq.add(new TermQuery(new Term("title", "science")), Occur.SHOULD)

      searcher.search(bq.build(), tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 3

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("The Art of Computer Science", "Lucene for Dummies", "Lucene for Dummies 2")
    }

    "Simple multi-term OR search, boost dummies" in {

      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val bq = new BooleanQuery.Builder()
      bq.add(new BoostQuery(new TermQuery(new Term("title", "dummies")), 2), Occur.SHOULD)
      bq.add(new TermQuery(new Term("title", "science")), Occur.SHOULD)

      searcher.search(bq.build(), tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 3

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("Lucene for Dummies", "Lucene for Dummies 2", "The Art of Computer Science")
    }


    "Simple phrase query" in {

      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val builder = new MultiPhraseQuery.Builder()
      builder.add(Seq(
        new Term("title", "lucene"),
        new Term("title", "science")
      ).toArray)

      searcher.search(builder.build(), tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 4

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("The Art of Computer Science", "Lucene in Action", "Lucene for Dummies", "Lucene for Dummies 2")
    }

    "Simple fuzzy query" in {

      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val q = new FuzzyQuery(new Term("title", "lucane"))

      searcher.search(q, tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 3

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("Lucene in Action", "Lucene for Dummies", "Lucene for Dummies 2")
    }

    "Simple prefix query" in {

      val searcher = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val q = new PrefixQuery(new Term("title", "luc"))

      searcher.search(q, tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 3

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSeq shouldBe Seq("Lucene in Action", "Lucene for Dummies", "Lucene for Dummies 2")
    }


  }
}
