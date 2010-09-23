{ 
   name : "NewRequest",
   type : "object",
   description : "The schema for a new Request object.",
   strict : true,
      
   properties : {
       
    search_text : { 
        type : "string", 
    },
    
    context : { 
        type : "string", 
    },
    
    provenance : { 
        type : "string",
    },
    
    comment : { 
        type : "string",
        optional : true,
    },
     
     ontology : { 
        type : "string",
        optional : true,
     },
     
     metadata : {
       type : "array",
       items : {
         type : "NewMetadata"
       }
     }
     
   }
}
