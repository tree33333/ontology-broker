{ 
   name : "Request",
   type : "object",
   description : "A Request is an entry signifying a request for a new term, lodged with the Ontology Broker, and intended to be satisified by an Ontology Maintainer through interactions with the same Broker.",
   
   properties : { 

    provisional_term : "string",
    ontology_term : "string",
       
    search_text : "string",
    context : "string", 
    provenance : "string",
    comment : "string",
    status : "string",

    
    creator : { 
        type : "Link",
        description : "A reference to the creator of this request.",
    },
    
    modified_by : "Link",
    
    date_submitted : "string",
     
     ontology : { 
        type : "Link",
     },
     
     metadata : {
       type : "array",
       items : {
         type : "Metadata"
       }
     }
     
   }
}
