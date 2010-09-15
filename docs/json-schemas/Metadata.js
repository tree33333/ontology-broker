
{
   name : "Metadata",
   type : "object",
   
   description : "A Metadata entry is a key-value pair, with provenance information, associated with a particular request. The metadata entry's key may be additionally parsed for identification of the value within a bulk-request submission.",
   
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
     
     created_on : { 
        type : "string",
        description : "A string field, of format yyyy-MM-dd, describing when this metadata value was first created and attached to the corresponding request.",
     },
     
     created_by : { 
         type : "Link",
         description : "A link to the URI identifying the user who created this metadata entry.",
     },
   }
 }