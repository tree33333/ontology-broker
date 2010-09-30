 { 
   name: "Supervisor", 
   type: "object",
   description: "Name and contact information for the supervisor of an ontology broker.",
   
   properties: {
     name : {
       type : "string", 
       description : "Personal name, typically first name and last name."
     },
     
    email : { 
        type : "string",
        description : "An email address for the supervisor.",
    },
   }
 }
