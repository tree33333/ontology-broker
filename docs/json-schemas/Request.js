
{ 
   "name" : "Request",
   "type" : "object",
   
   "properties" : { 
   
     "search_text" : "string",
     "context" : "string", 
     "provenance" : "string",
     "date_submitted" : "string",
     "status" : "integer",
     "ontology_id" : "string",
     "provisional_id" : "string",
     
     "created_by" : "integer",
     
     "metadata" : {
       "type" : "array",
       "items" : {
         "type" : "Metadata"
       }
     }
     
   }
}
