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
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.facet.taxonomy.directory.{DirectoryTaxonomyReader, DirectoryTaxonomyWriter}
import org.apache.lucene.facet.{FacetField, FacetsCollector, FacetsConfig}
import org.apache.lucene.index._
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import uk.gov.hmrc.play.test.UnitSpec

class LuceneFacetTaxonomySpec extends UnitSpec {

  "Wibble" should {

    val analyzer = new StandardAnalyzer();

    def buildIndex() = {
      val config = new IndexWriterConfig(analyzer);
      val index: Directory = new RAMDirectory();
      val w = new IndexWriter(index, config);

      // Writes facet ords to a separate directory from the main index
      val taxonomyIndex: Directory = new RAMDirectory()
      val facetConfig = new FacetsConfig()
      val tw = new DirectoryTaxonomyWriter(taxonomyIndex)


      def addDoc(title: String, isbn: String, sector: String) {
        val doc = new Document
        doc.add(new TextField("title", title, Field.Store.YES))
        doc.add(new StringField("isbn", isbn, Field.Store.YES))
        doc.add(new FacetField("sector", sector))
        w.addDocument(facetConfig.build(tw, doc))
      }

      addDoc("Lucene in Action", "193398817", "Sector 1");
      addDoc("Lucene for Dummies", "55320055Z", "Sector 1");
      addDoc("Lucene for Dummies 2", "55320055Z", "Sector 2");
      addDoc("Managing Gigabytes", "55063554A", "Sector 2");
      addDoc("The Art of Computer Science", "9900333X", "Sector 3");

      tw.close()
      w.close();

      (index, taxonomyIndex)
    }

    "Calculate facets over whole index" in {
      val facetConfig = new FacetsConfig()
      val (index, taxonomyIndex) = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val taxoReader = new DirectoryTaxonomyReader(taxonomyIndex)

      val searcher = new IndexSearcher(reader)

      val collector = new FacetsCollector()

      FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, collector)

      val facets = new FastTaxonomyFacetCounts(taxoReader, facetConfig, collector)

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 2, "Sector 3" -> 1)
    }

    "search for lucene and get results plus facets for results" in {
      val facetConfig = new FacetsConfig()
      val (index, taxonomyIndex) = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val taxoReader = new DirectoryTaxonomyReader(taxonomyIndex)

      val q = new TermQuery(new Term("title", "lucene"))

      val searcher = new IndexSearcher(reader)

      val tdc = TopScoreDocCollector.create(10)
      val fc = new FacetsCollector()

      searcher.search(q, MultiCollector.wrap(tdc, fc))

      val results = tdc.topDocs(0, 5)
      results.totalHits shouldBe 3

      results.scoreDocs.map{
        result =>
          val doc = searcher.doc(result.doc)
          doc.get("title")
      }.toSet shouldBe Set("Lucene in Action", "Lucene for Dummies", "Lucene for Dummies 2")


      val facets = new FastTaxonomyFacetCounts(taxoReader, facetConfig, fc)

      val facetResult = facets.getTopChildren(10, "sector").labelValues map {
        lv => lv.label -> lv.value
      }

      facetResult.toSeq shouldBe Seq("Sector 1" -> 2, "Sector 2" -> 1)
    }
  }
}
