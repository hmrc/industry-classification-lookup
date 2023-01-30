# industry-classification-lookup

[![Build Status](https://travis-ci.org/hmrc/industry-classification-lookup.svg)](https://travis-ci.org/hmrc/industry-classification-lookup) [ ![Download](https://api.bintray.com/packages/hmrc/releases/industry-classification-lookup/images/download.svg) ](https://bintray.com/hmrc/releases/industry-classification-lookup/_latestVersion)


## Lookup

The lookup API provides a simple API to map from a 5 digit code to the description
related to the code.

| Path                                                   | Supported Methods | Description  |
| -------------------------------------------------------| ------------------| ------------ |
|```/industry-classification-lookup/lookup/:sicCode```   |        GET        | Retrieves the sic code description for the supplied sic code if it exists|

Responds with:

| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 404           | Not found     |

Headers:

* Authorization - a valid BEARER token for a signed in user.

Example URLs:

* /industry-classification-lookup/lookup/01621

Example success response:

```json
{
   "code": "01621",
   "desc": "Farm animal boarding and care"
}
```

## Search

| Path                                          | Supported Methods | Description  |
| ----------------------------------------------| ------------------| ------------ |
|```/industry-classification-lookup/search```   |        GET        | Searches for the sic codes that match the query|

Request parameters:
* `query` - mandatory - the text to search for
* `queryParser` - optional - true/false, if `true` the query is treated as a raw Lucene query else a simple query where each word is searched with a simple OR construction (`query-builder`)
* `queryBoostFirstTerm` - optional - true/false, if `true` the first word is boosted to bring results with that term closer to the top of the results else simple query (`query-builder`)
* `pageResults` - optional - the number of results to return per page - default number returned is `5`
* `page` - optional - which page to return - default returns first page
* `indexName` - optional - which index to use, must be one of these
  * `gds-register-sic5` - 5 digit codes from the GDS register
  * `ons-supplement-sic5` - ONS 5 digit codes with supplementary indices to help with searching
* `sector` - optional - perform a facet search to restrict to results from a single industry sector
  * value should be a code returned in the `sectors` array of a previous result

Notes:
* If both `queryParser` and `queryBoostFirstTerm` are not provided, the default search will be a simple `query-builder`
* A first search is executed with the parameters provided if no results are found, a second search is executed automatically with `fuzzy-query`
  * `fuzzy-query` - simple query (like `query-builder`), but if no results are found the terms are made _fuzzy_ - so that near matches are found, e.g. `ferm` would find `farm`
  * `fuzzy-query` is not executed if `queryParser` is set to `true`

Indexes:

Searches using the `ons-supplement-sic5` index return descriptions that
are not strictly SIC descriptions, but instead results in the index that may help
to select the correct SIC code.

To get true SIC descriptions, either call the `lookup` API with the code, or
use the `gds-register-sic5` index. Bear in mind that research has shown that
finding the correct SIC code by searching against the shorter `gds-register-sic5`
index is **much** more difficult.


Headers:

* Authorization - a valid BEARER token for a signed in user

Responds with:

| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 404           | Not found     |


### Example - simple search

* `numFound` is the number in the results - paging may make the number in the `results` field to be fewer than this value
* `nonFilteredFound` is the same as `numFound` because there's no filtering
* `results` is the set of SIC codes and descriptions to show
* `sectors` is the list of sectors in the original un-filtered results

This search is using the `ons-supplement-sic5` index and therefore the descriptions
are not strictly SIC descriptions, but instead results in the index that may help
to select the correct SIC code.

URL:

* /industry-classification-lookup/search?pageResults=5&query=farm&queryType=query-parser&indexName=ons-supplement-sic5

Response:

```json
{
  "numFound":16,
  "nonFilteredFound":16,
  "results":[
    {"code":"37000","desc":"Sewage farm"},
    {"code":"96040","desc":"Health farm"},
    {"code":"01470","desc":"Chicken farm (battery rearing)"},
    {"code":"01621","desc":"Farm animal boarding and care"},
    {"code":"46610","desc":"Dairy farm machinery (wholesale)"}
  ],
  "sectors":[
    {"code":"A","name":"Agriculture, Forestry And Fishing","count":5},
    {"code":"C","name":"Manufacturing","count":5},
    {"code":"G","name":"Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles","count":3},
    {"code":"E","name":"Water Supply; Sewerage, Waste Management And Remediation Activities","count":1},
    {"code":"M","name":"Professional, Scientific And Technical Activities","count":1},
    {"code":"S","name":"Other Service Activities","count":1}
  ]
}
```

### Example - facet search by sector `G`

Results are limited to those in the selected sector (i.e. Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles).
The sectors returns are from the original search to allow refinement to an alternate sector.

* `numFound` is the number in the sector (regardless of the number in the `results` array - which could be shorter due to paging)
* `nonFilteredFound` is the original number of results without the filtering
* `results` is the set of SIC codes and descriptions to show
* `sectors` is the list of sectors in the original un-filtered results

URL:

* /industry-classification-lookup/search?&query=farm&queryType=query-parser&indexName=ons-supplement-sic5&sector=G

Response:

```json
{
  "numFound":3,
  "nonFilteredFound":16,
  "results":[
    {"code":"46610","desc":"Dairy farm machinery (wholesale)"},
    {"code":"46690","desc":"Dairy machinery (not farm) (wholesale)"},
    {"code":"46210","desc":"Prepared feeds for farm animals (wholesale)"}
  ],
  "sectors":[
    {"code":"A","name":"Agriculture, Forestry And Fishing","count":5},
    {"code":"C","name":"Manufacturing","count":5},
    {"code":"G","name":"Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles","count":3},
    {"code":"E","name":"Water Supply; Sewerage, Waste Management And Remediation Activities","count":1},
    {"code":"M","name":"Professional, Scientific And Technical Activities","count":1},
    {"code":"S","name":"Other Service Activities","count":1}
  ]
}
```
## Running locally
User service manager to run all services required by ICL backend:

```bash
sm --start ICL_ALL -f
```
Note this will start the ICL backend itself too, as it's included in the profile.

Alternatively, to run the service with local changes, `cd` to cloned directory and execute following:

- `sm --stop ICL`
- `/run.sh`

The service will come to life  @
http://localhost:9875/

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")