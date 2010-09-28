 { 
   name: "OntologyField", 
   type: "object",
   
   properties: {
   
    field_name : {
       type : "string", 
       description : ""
     },
     
    field_description : {
       type : "string", 
       description : ""
     },
     
    field_metadata_key : {
       type : "string", 
       description : ""
     },
     

    ontology : { 
        type : "Link",
        description : "A reference to ontology for which this is a field.",
    },
   }
 }
