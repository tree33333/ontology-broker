{ 
   name : "Request",
   type : "object",
   description : "A Request is an entry signifying a request for a new term, lodged with the Ontology Broker, and intended to be satisified by an Ontology Maintainer through interactions with the same Broker.",
   
   properties : { 

    provisional_term : { 
        type : "string",
    },
    
    ontology_term : { 
        type : "string",
        optional : true,
    },
       
    search_text : { 
        type : "string", 
    },
    
    context : { 
        type : "string", 
    },
    
    provenance : { 
        type : "string",
        optional : true,
    },
    
    comment : { 
        type : "string",
        optional : true,
    },
    
    status : { 
        type : "string",
    },

    creator : { 
        type : "Link",
        description : "A reference to the creator of this request.",
    },
    
    modified_by : { 
        type : "Link",
    },
    
    date_submitted : { 
        type : "string",
    },
     
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
