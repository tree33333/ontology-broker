/*
   Copyright 2010 Massachusetts General Hospital

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.sc.probro;

import java.util.Collection;

import org.json.JSONObject;
import org.junit.experimental.theories.Theories;
import org.sc.obo.OBOOntology;
import org.sc.probro.exceptions.BadRequestException;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.exceptions.GoneException;

/**
 * 
 * Broker is the central interface to a broker instance, running either locally or remotely.
 * 
 * The methods available in this interface should roughly correspond to the functions defined
 * in the <a href="http://neurocommons.org/page/Ontological_term_broker#Functional_Requirements">design
 * document</a>.  
 * 
 * @author Timothy Danford
 * @date 9/16/2010
 */
public interface Broker {
	
	public void close() throws BrokerException;
	
	/**
	 * <font style="color: red;">Corresponds to none of the functional requirements.</font>
	 * 
	 * Looks up an Ontology object by its string identifier.
	 * 
	 * @param creds The user calling this method.
	 * @param ontologyID The string identifier of the Ontology.
	 * @return The Ontology object corresponding to the given identifier.
	 * @throws BrokerException If the broker is unavailable, or an internal error occurs.
	 * @throws GoneException If the <tt>ontologyID</tt> argument corresponds to no known Ontology.
	 */
	public Ontology checkOntology(UserCredentials creds, String ontologyID) throws BrokerException, GoneException;
	
	public Ontology createOntology(UserCredentials creds, String ontologyName, OBOOntology ontology) throws BrokerException;
	
	/**
	 * <font style="color: red;">Corresponds to none of the functional requirements.</font>
	 * 
	 * Looks up a User object by its string identifier.
	 * 
	 * @param creds The user calling this method.
	 * @param userID The string identifier of the User.
	 * @return The User object corresponding to the given identifier.
	 * @throws BrokerException If the broker is unavailable, or an internal error occurs.
	 * @throws GoneException If the <tt>ontologyID</tt> argument corresponds to no known Ontology.
	 */
	public User checkUser(UserCredentials creds, String userID) throws BrokerException, GoneException;

	/**
	 * From functional requirements: 
	 * <blockquote><i>Lists the available ontologies against which requests may be made using this broker.</i></blockquote>
	 * 
	 * @param user The credentials of the user making this call.
	 * @return An array of Ontology objects, representing the ontologies for which this broker will accept requests.
	 * @throws BrokerException
	 */
	public Ontology[] listOntologies(UserCredentials user) throws BrokerException;

	/**
	 * From functional requirements: 
	 * <blockquote><i>Search for an existing term or pending query.</i></blockquote>
	 *
	 * Allows a user to search, through a string-based text-matching service, through existing requests
	 * and extant terms in the given ontology for a term or request matching the user's needs.
	 * 
	 * @param user The user making the query.
	 * @param search The 'search string', which will be matched against pending requests and existing terms. 
	 * @param ontology_id One or more ontologies in which to search; if unspecified, the search will match any ontology.
	 * @return An array of SearchResult objects, representing the 'hits' which matched the search term.
	 * @throws BrokerException If the service is unavailable.
	 * @throws GoneException If an ontology_id parameter corresponds to an unknown ontology.
	 */
	public SearchResult[] query(
			UserCredentials user, 
			String search, 
			String... ontology_id) throws BrokerException, GoneException;

	/**
	 * From functional requirement: 
	 * <blockquote><em>Create request for a new term.</em></blockquote>
	 * 
	 * Creates a new term request against the indicated ontology.  If the request is successfully created, 
	 * it is given the status of <tt>PENDING</tt> and will be returned to the Ontology Maintainer for the 
	 * corresponding ontology at his or her next attempt to list all outstanding requests in bulk.
	 * 
	 * @param user The user creating the request.
	 * @param name The 'name' of the term requested; should be a short, descriptive string.
	 * @param context The 'context' of the term requested; should be a larger block of text, corresponding to the setting in which the 'name' string was originally found or referenced.
	 * @param provenance A string indicator of provenance, likely a URL, indicating where the 'context' argument was located (if available). 
	 * @param ontology The ontology against which the request should be created.
	 * @param metadatas A list of metadata values, key-value pairs which contain additional information about the term requested.
	 * @return A {@link java.lang.String} representing the provisional identifier to be used for denoting the requested term, until a new identifier is assigned by the ontology maintainer. 
	 * @throws BrokerException
	 * @throws GoneException If the given ontology does not correspond to a known Ontology.
	 * @throws BadRequestException If any of the required fields (name, context, provenance) are missing, or if any ontology-specific metadata are poorly formatted.
	 */
	public String request(
			UserCredentials user, 
			String name, 
			String context, 
			String provenance, 
			Ontology ontology,
			Collection<Metadata> metadatas) throws BrokerException, GoneException, BadRequestException;
	
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
	
	public BulkTable listRequestsInBulk(
			UserCredentials user, 
			String ontology) throws BrokerException;
	
	public void respondInBulk(UserCredentials user, BulkTable response) throws BrokerException;
	
	public Request[] listRequests(UserCredentials user, Ontology ontology) throws BrokerException;

	public void addOntologyField(UserCredentials user, Ontology ont, OntologyField newField) throws BrokerException;
}
