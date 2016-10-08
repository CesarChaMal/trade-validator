Trade validation service
========================


Example API calls
=================

curl  -v -X POST localhost:8080/api/validate

curl  -v -H "Content-Type: application/json" -X POST -d '{"customer":"PLUTO1","ccyPair":"EURUSD","type":"Spot","direction":"BUY","tradeDate":"2016-08-11","amount1":1000000.00,"amount2":1120000.00,"rate":1.12,"valueDate":"2016-08-15","legalEntity":"CS Zurich","trader":"Johann Baumfiddler"}' localhost:8080/api/validate


curl http://localhost:8080/api/shutdown

curl -X POST http://localhost:8080/api/shutdown

curl -v http://localhost:8080/info

http://localhost:8080/metrics

API documentation
=================

http://localhost:8080/swagger-ui.html


http://localhost:8080/swagger-ui.html#/validation-controller