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
     
     
    maintainer : { 
        type : "Link",
        description : "A reference to the maintainer of this ontology.",
    },
    
    fields : { 
        type : "array",
        items : { 
            type : "OntologyField",
        },
        description: "The fields which define the metadata that must be associated with each request to this ontology, and the mapping of those metadata to the bulk request table of the ontology.",
    },
   }
 }
