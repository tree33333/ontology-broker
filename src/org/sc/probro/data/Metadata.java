package org.sc.probro.data;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.sql.*;

import org.sc.probro.data.DBObject;

public class Metadata extends DBObject {
	
	public Integer request_id;
	public String metadata_key, metadata_value;
	
	public Metadata() { super(); }
	public Metadata(ResultSet rs) throws SQLException { super(rs); }
	
	public boolean isAutoGenerated(String fieldName) {
		return false;
	}
	
	public String toString() { 
		return String.format("%d:%s=\"%s\"", request_id, metadata_key, metadata_value);
	}
}