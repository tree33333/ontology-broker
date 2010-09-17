package org.sc.probro;

import java.util.Collection;

import org.json.JSONObject;
import org.sc.probro.exceptions.BrokerException;

public interface Broker {
	
	public Ontology checkOntology(UserCredentials creds, String ontologyID) throws BrokerException;
	public User checkUser(UserCredentials creds, String userID) throws BrokerException;
	
	/*
	 * Annotator Methods
	 */

	public Ontology[] listOntologies(UserCredentials user) throws BrokerException;
	
	public SearchResult[] query(
			UserCredentials user, 
			String search, 
			String... ontology_id) throws BrokerException;
	
	public String request(
			UserCredentials user, 
			String name, 
			String context, 
			String provenance, 
			Collection<Metadata> metadatas) throws BrokerException;
	
	public void update(UserCredentials user, 
			String request, Request requestData) throws BrokerException;
	
	public Request check(UserCredentials user, String request) throws BrokerException;
	
	public void judge(UserCredentials user, String request, String judgment, String comment) throws BrokerException;
	
	/*
	 * Maintainer Methods
	 */

	public User[] listUsers(
			UserCredentials user, 
			String ontology) throws BrokerException;
	
	public BulkRequestTable listRequestsInBulk(
			UserCredentials user, 
			String ontology) throws BrokerException;
	
	public void respondInBulk(UserCredentials user, BulkResponseTable response) throws BrokerException;
	
	public Request[] listRequests(UserCredentials user, String ontologyID) throws BrokerException;
}
