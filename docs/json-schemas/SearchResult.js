 {
    name : "SearchResult",
    type : "object",
    
    properties : { 
  
        id : "string",
        response_type : "string",  
             
        description : {
            type : "array",
            items : { type : "string" }
        },
        
        accession : {
            type : "array",
            items : { type : "string" }
        }
  }
}
