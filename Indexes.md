# industry-classification-lookup - Indexes

## create pipe delimited from JSON

Run :
```cat conf/sic-codes.json |jq -r '.sectors[].sics[] | "\(.code)|\(.desc)"' > conf/sic-codes.txt```

To convert JSON in this format :
```
01140001|Sugar cane growing
01410001|Buffalo milk, raw
01410002|Cows' milk (raw) production
...
```

To convert JSON in this format :
```
{"sectors":
[{
  "sics": [
    {"code": "01140001", "desc": "Sugar cane growing"},
    {"code": "01410001", "desc": "Buffalo milk, raw"},
    {"code": "01410002", "desc": "Cows' milk (raw) production"},
...
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")