# industry-classification-lookup

[![Build Status](https://travis-ci.org/hmrc/industry-classification-lookup.svg)](https://travis-ci.org/hmrc/industry-classification-lookup) [ ![Download](https://api.bintray.com/packages/hmrc/releases/industry-classification-lookup/images/download.svg) ](https://bintray.com/hmrc/releases/industry-classification-lookup/_latestVersion)


| Path                                                   | Supported Methods | Description  |
| -------------------------------------------------------| ------------------| ------------ |
|```/industry-classification-lookup/lookup/:sicCode```   |        GET        | Retrieves the sic code description for the supplied sic code if it exists|

Responds with:

| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 404           | Not found     |


A ```200``` success response:

```json
{
   "code": "12345678",
   "desc": "This is a sic code description"
}
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")