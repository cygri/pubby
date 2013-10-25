# A Pubby configuration for Linked Open Vocabularies

**This branch is for @cygri's personal use. Pubby development happens on `master` and on feature branches.**

This branch contains a Pubby configuration that sets up a site on http://localhost:8080/ with data from [lov.okfn.org](http://lov.okfn.org). It allows browsing of an impressive collection of vocabularies. This is very much work in progress.

The first few page loads are *really* slow because vocabulary descriptions are being retrieved and cached. It gets faster.

## TODO

* Do something about the slow label lookup
* Customize all SPARQL queries to use the clever `GRAPH` stuff

## Issues with the LOV SPARQL endpoint

1. The endpoint defaults to serving HTML when no format is selected (`&format=XXX` not present in the query URL). This breaks the SPARQL Protocol, and compliant SPARQL clients can't access the endpoint. When no format is specified, the standard XML result format and Turtle or RDF/XML should be used. Or perhaps content negotiation. Anyway, not HTML.
2. Syntax errors should be reported with an HTTP 400 status code. SPARQL clients rely on the status code to detect that there was an error. Without that status code, SPARQL clients will think that all is fine and will try to parse the error message as a normal result.
