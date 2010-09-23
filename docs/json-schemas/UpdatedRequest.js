{ 
   name : "UpdatedRequest",
   type : "object",
   description : "The schema for an updated Request object.",
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

    creator : { 
        type : "string",
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
