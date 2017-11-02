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

package lucene

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.facet.sortedset.{DefaultSortedSetDocValuesReaderState, SortedSetDocValuesFacetCounts, SortedSetDocValuesFacetField}
import org.apache.lucene.facet.{DrillDownQuery, DrillSideways, FacetsCollector, FacetsConfig}
import org.apache.lucene.index._
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import uk.gov.hmrc.play.test.UnitSpec

class LuceneFacetSortedSetSpec extends UnitSpec {

  "Wibble" should {

    val analyzer = new StandardAnalyzer();

    def buildIndex() = {
      val config = new IndexWriterConfig(analyzer);
      val index: Directory = new RAMDirectory();
      val w = new IndexWriter(index, config);

      val facetConfig = new FacetsConfig()

      def addDoc(title: String, isbn: String, sector: String) {
        val doc = new Document
        doc.add(new TextField("title", title, Field.Store.YES))
        doc.add(new StringField("isbn", isbn, Field.Store.YES))
        doc.add(new SortedSetDocValuesFacetField("sector", sector))
        w.addDocument(facetConfig.build(doc))
      }

      addDoc("Lucene in Action", "193398817", "Sector 1");
      addDoc("Lucene for Dummies", "55320055Z", "Sector 1");
      addDoc("Lucene for Dummies 2", "55320055Z", "Sector 2");
      addDoc("Managing Gigabytes", "55063554A", "Sector 2");
      addDoc("The Art of Computer Science", "9900333X", "Sector 3");

      w.close();

      index
    }

    def setupSearch() = {
      val facetConfig = new FacetsConfig()
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)

      val searcher = new IndexSearcher(reader)
      val state = new DefaultSortedSetDocValuesReaderState(reader)

      (searcher, state, facetConfig)
    }

    "Calculate facets over whole index" in {

      val (searcher, state, facetConfig) = setupSearch()

      val collector = new FacetsCollector()

      FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, collector)

      val facets = new SortedSetDocValuesFacetCounts(state, collector)

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 2, "Sector 3" -> 1)
    }

    "search for lucene and get results plus facets for results" in {

      val (searcher, state, facetConfig) = setupSearch()

      val q = new TermQuery(new Term("title", "lucene"))

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      searcher.search(q, MultiCollector.wrap(tdc, fc))

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 3

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSet shouldBe Set("Lucene in Action", "Lucene for Dummies", "Lucene for Dummies 2")

      val facets = new SortedSetDocValuesFacetCounts(state, fc)

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 1)
    }

    "search twice for 'lucene' to get facets and then for results with a drill down into Sector 2" in {

      val (searcher, state, facetConfig) = setupSearch()

      val q = new TermQuery(new Term("title", "lucene"))

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      searcher.search(q, fc)

      val facets = new SortedSetDocValuesFacetCounts(state, fc)

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 1)

      val ddq = new DrillDownQuery(facetConfig, q)
      ddq.add("sector", "Sector 2")
      searcher.search(ddq, tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 1

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSet shouldBe Set("Lucene for Dummies 2")
    }

    "Calculate facets over whole index, drill sideways" in {

      val (searcher, state, facetConfig) = setupSearch()

      val fc = new FacetsCollector()

      val q = new DrillDownQuery(facetConfig)
      q.add("sector", "Sector 2")

      val ds = new DrillSideways(searcher, facetConfig, state)
      val result = ds.search(q, 10)

      val facets = result.facets

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 2, "Sector 3" -> 1)
    }

    "Calculate facets over whole index, drill sideways into 'Sector 2'" in {

      val (searcher, state, facetConfig) = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val q = new DrillDownQuery(facetConfig)
      q.add("sector", "Sector 2")

      val ds = new DrillSideways(searcher, facetConfig, state)
      val result = ds.search(q, tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 2

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSet shouldBe Set("Lucene for Dummies 2", "Managing Gigabytes")

      val facets = result.facets

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 2, "Sector 3" -> 1)
    }

    "Calculate facets for 'lucene', drill sideways into 'Sector 2'" in {

      val (searcher, state, facetConfig) = setupSearch()

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      val q = new TermQuery(new Term("title", "lucene"))
      val ddq = new DrillDownQuery(facetConfig, q)
      ddq.add("sector", "Sector 2")

      val ds = new DrillSideways(searcher, facetConfig, state)
      val result = ds.search(ddq, tdc)

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 1

      results.scoreDocs.map {
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSet shouldBe Set("Lucene for Dummies 2")

      val facets = result.facets

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 1)
    }
  }
}
