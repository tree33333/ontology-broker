package org.sc.probro.data;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.sql.*;

import org.sc.probro.Ontology;
import org.sc.probro.data.DBObject;

/**
CREATE TABLE ONTOLOGYFIELDS (
field_id        int primary key generated by default as identity,
ontology_id     int not null references ONTOLOGYS(ontology_id) ON DELETE CASCADE,
field_name      varchar(100) not null,
field_description  clob,
field_metadata_key  varchar(100)
);

 * @author tdanford
 *
 */
public class OntologyFieldObject extends DBObject {
	
	public Integer field_id;
	public Integer ontology_id;
	public String field_name, field_description, field_metadata_key;
	
	public OntologyFieldObject() { super(); }
	public OntologyFieldObject(ResultSet rs) throws SQLException { super(rs); }
	
	public boolean isAutoGenerated(String fieldName) {
		return fieldName.toLowerCase().equals("field_id");
	}

	public String getKey() { return "field_id"; }
	
	public String[] getAutoGeneratedFields() {
		return new String[] { "field_id" };
	}
	
	public String[] getKeyFields() {
		return getAutoGeneratedFields();
	}
	
	
}
