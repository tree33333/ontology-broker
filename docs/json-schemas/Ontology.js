 { 
   name: "Ontology", 
   type: "object",
   
   properties: {
     name : {
       type : "string", 
       description : "Human-readable Ontology Name"
     },
     
     href : { 
        type : "Link",
        description : "Link to Ontology entry on broker",
     },
   }
 }
