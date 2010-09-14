
{ 
   "name" : "Request",
   "properties" : { 
     "request_id" : "integer",
     "search_text" : "string",
     "context" : "string", 
     "provenance" : "string",
     "date_submitted" : "string",
     "response_code" : "integer",
     "ontology_id" : "integer",
     "provisional_id" : "string",
     "metadata" : {
       "type" : "array",
       "items" : {
         "type" : Metadata
       }
     }
   }
}