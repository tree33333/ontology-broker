
{
   name : "NewMetadata",
   type : "object",
   strict : true,
   
   description : "A Schema for a New Metadata entry.",
   
   properties : { 
   
     metadata_key : { 
        type : "string",
        description : 
            "The string by which this metadata entry may be identified.",
     },
     
     metadata_value : { 
        type : "string",
        description : "The value of this metadata entry.",
     },
   }
 }