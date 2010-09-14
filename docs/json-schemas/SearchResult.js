 {
  "name" : "SearchResult",
  "type" : "object",
  "properties" : { 
     "id" : "string",
     "type" : "string",   // "type" -> "response-type", see below.
     "description" : {
       "type" : "array",
       "items" : { "type" : "string" }
     },
     "accession" : {
       "type" : "array",
       "items" : { "type" : "string" }
     }
  }
}
