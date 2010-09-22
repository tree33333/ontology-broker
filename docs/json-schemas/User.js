{ 
    name: "User", 
    type: "object",
   
    properties: {
     
        user_name : {
            type : "string", 
            description : "Human-readable User Name"
        },
     
        href : { 
            type : "Link",
            description : "Link to User entry", 
        },
    }
}