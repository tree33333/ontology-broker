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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.*;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sc.obo.OBOOntology;
import org.sc.obo.OBOStanza;
import org.sc.obo.OBOValue;
import org.sc.probro.data.*;
import org.sc.probro.exceptions.*;
import org.sc.probro.lucene.IndexCreator;
import org.sc.probro.lucene.ProteinSearcher;
import org.sc.probro.servlets.RequestStateServlet;

public class LocalBroker implements Broker {
	
	private BrokerModel model;
	private String luceneIndexPath;
	
	public LocalBroker(BrokerProperties props, BrokerModel m) {
		luceneIndexPath = props.getLuceneIndex();
		model = m;
	}
	
	public void close() throws BrokerException {
		try { 
			model.close();
		} catch(DBModelException e) { 
			throw new BrokerException(e);
		}
	}
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	public String formatDate(Date d) { 
		return dateFormat.format(d);
	}
	
	public Ontology createOntology(UserCredentials creds, String ontologyName, OBOOntology ontology) throws BrokerException { 
		
		UserObject maintainer = new UserObject();
		
		maintainer.user_id = 1;
		
		try {
			OntologyObject ontologyObject = null;
			
			try { 
				model.startTransaction();

				ontologyObject = model.createNewOntology(ontologyName, maintainer);

				addOntologyToIndex(ontologyName, ontology);

				model.commitTransaction();
			
			} catch(DBModelException e) { 
				model.rollbackTransaction();
				throw e;
			}
			
			String ontologyID = ontologyURL(ontologyObject.ontology_id);
			
			assert ontologyID != null;
			
			return checkOntology(creds, ontologyID);
			
		} catch (DBModelException e) {
			throw new BrokerException(e);
		}
	}
	
	private void addOntologyToIndex(String type, OBOOntology ontology) throws BrokerException {
		
		Collection<OBOStanza> stanzas = ontology != null ? ontology.getStanzas() : 
			new LinkedList<OBOStanza>();

		File dir = new File(luceneIndexPath);

		try { 
			IndexCreator creator = new IndexCreator(dir);
			try { 
				for(OBOStanza stanza : stanzas) { 
					String id = stanza.id();
					
					Set<String> accs = new TreeSet<String>();
					Set<String> descs = new TreeSet<String>();
					
					Pattern p = Pattern.compile("\"(.*)\" (EXACT)|(RELATED) \\[\\]");
					
					List<OBOValue> values = stanza.values("synonym");					
					if(values != null) { 
						for(OBOValue value : values) { 
							Matcher m = p.matcher(value.getValue());
							if(m.matches()) { 
								accs.add(m.group(1));
							} else { 
								accs.add(value.getValue());
							}
						}
					}
					
					values = stanza.values("name");
					if(values != null) { 
						descs.add(stanza.values("name").get(0).getValue());
					}
					
					values = stanza.values("def");
					if(values != null) { 
						descs.add(stanza.values("def").get(0).getValue());
					}

					values = stanza.values("xref");
					if(values != null) { 
						for(OBOValue value : values) { 
							accs.add(value.getValue());
						}
					}

					if(creator != null) { 
						creator.addTerm(id, type, accs, descs);
					} else { 
						//Log.info(String.format("INDEX: %s (%s) %s %s", id, type, String.valueOf(accs), String.valueOf(descs)));
					}
				}

			} finally {
				if(creator != null) { 
					try { 
						creator.checkpoint();
						creator.close();
					} catch(IOException e) { 
						throw new BrokerException(e); 
					}
				}
			}

		} catch(IOException e) { 
			throw new BrokerException(e);
		}

	}
	
	public Ontology checkOntology(UserCredentials creds, String ontologyID) throws BrokerException { 
		Ontology ont = new Ontology();
		
		OntologyObject template = new OntologyObject();
		template.ontology_id = parseOntologyID(ontologyID);
		Log.info(String.format("Loading requests from ontologyID=%d", template.ontology_id));
		
		try { 
			OntologyObject obj = model.getModel().loadOnly(OntologyObject.class, template);
			
			return convertOntology(obj);
		
		} catch (DBModelException e) {
			throw new BrokerException(e);
			
		} catch (DBObjectMissingException e) {
			throw new GoneException(ontologyID);
		}
	}
	
	public User checkUser(UserCredentials creds, String userID) throws BrokerException { 
		User user = new User();
		
		UserObject template = new UserObject();
		template.user_id = parseUserID(userID);
		try {
			UserObject obj = model.getModel().loadOnly(UserObject.class, template);

			return convertUser(obj);
		
		} catch (DBModelException e) {
			throw new BrokerException(e);
			
		} catch (DBObjectMissingException e) {
			throw new GoneException(userID);
		}
	}
	
	public String ontologyURL(int id) { 
		return model.url(String.format("/ontology/%d/", id));
	}
	
	public String userURL(int id) { 
		return model.url(String.format("/user/%d/", id));
	}
	
	private static Pattern number = Pattern.compile("\\d+");
	
	public Integer parseUserID(String url) { 
		if(number.matcher(url).matches()) { return Integer.parseInt(url); }
		Pattern p = Pattern.compile("^.*/user/([^\\/]+)/?.*$");
		Matcher m = p.matcher(url);
		if(!m.matches()) { throw new IllegalArgumentException(url); }
		return Integer.parseInt(m.group(1));
	}
	
	public Integer parseOntologyID(String url) { 
		if(number.matcher(url).matches()) { return Integer.parseInt(url); }
		Pattern p = Pattern.compile("^.*/ontology/?([^\\/]+)/?.*$");
		Matcher m = p.matcher(url);
		if(!m.matches()) { throw new IllegalArgumentException(url); }
		return Integer.parseInt(m.group(1));
	}
	
	private Ontology loadOntology(int ontology_id) throws DBModelException, DBObjectMissingException { 
		DBObjectModel db = model.getModel();
		OntologyObject template = new OntologyObject();
		template.ontology_id = ontology_id;
		return convertOntology(db.loadOnly(OntologyObject.class, template));
	}
	
	private Ontology convertOntology(OntologyObject ontObj) throws DBModelException, DBObjectMissingException { 
		Ontology ont = new Ontology();
		
		DBObjectModel db = model.getModel();
		
		ont.id = ontologyURL(ontObj.ontology_id);
		ont.ontology_name = ontObj.ontology_name;
		ont.maintainer = loadUser(ontObj.maintainer_id);
		
		ont.fields = new ArrayList<OntologyField>();
		OntologyFieldObject template = new OntologyFieldObject();
		template.ontology_id = ontObj.ontology_id;
		
		for(OntologyFieldObject fieldObj : db.load(OntologyFieldObject.class, template)) {  
			ont.fields.add(convertOntologyField(fieldObj));
		}
		
		return ont;
	}
	
	private User loadUser(int user_id) throws DBModelException, DBObjectMissingException { 
		DBObjectModel db = model.getModel();
		UserObject template = new UserObject();
		template.user_id = user_id;
		return convertUser(db.loadOnly(UserObject.class, template));
	}
	
	private User convertUser(UserObject userObj) { 
		User user = new User();
		user.id = userURL(userObj.user_id);
		user.user_name = userObj.user_name;
		return user;
	}
	
	private Collection<MetadataObject> loadMetadatas(int reqID) throws DBModelException { 
		MetadataObject template = new MetadataObject();
		template.request_id = reqID;
		return model.getModel().load(MetadataObject.class, template);
	}
	
	private Metadata convertMetadata(MetadataObject metaObj) throws DBModelException, DBObjectMissingException { 
		Metadata meta = new Metadata();
		meta.created_by = loadUser(metaObj.created_by);
		meta.created_on = formatDate(metaObj.created_on);
		meta.key = metaObj.metadata_key;
		meta.value = metaObj.metadata_value;
		return meta;
	}
	
	private OntologyField convertOntologyField(OntologyFieldObject obj) { 
		OntologyField field = new OntologyField();
		
		field.description = obj.field_description;
		field.name = obj.field_name;
		field.metadata_key = obj.field_metadata_key;
		
		return field;
	}
	
	private void unconvertRequest(RequestObject base, Request req) throws BadRequestException {
		
		base.ontology_term = req.ontology_term;
		base.search_text = req.search_text;
		base.context = req.context;
		base.comment = req.comment;
		try {
			base.date_submitted = dateFormat.parse(req.date_submitted);
		} catch (ParseException e) {
			throw new BadRequestException(String.format("Bad date string \"%s\", error message \"%s\"", 
					String.valueOf(req.date_submitted), e.getMessage()));
		}
		
		if(req.status==null) { 
			throw new BadRequestException("Request status was not specified.");
		}
	
		base.status = RequestStateServlet.STATES.forward(req.status);
		base.modified_by = parseUserID(req.modified_by.id);
		base.creator_id = parseUserID(req.creator.id);
	}
	
	private Collection<MetadataObject> unconvertMetadata(Collection<Metadata> metas) throws BadRequestException {
		if(metas == null) { throw new IllegalArgumentException("Null metadata list."); }
		
		LinkedList<MetadataObject> list = new LinkedList<MetadataObject>();

		for(Metadata m : metas) { 
			MetadataObject obj = new MetadataObject();
			
			if(m.key == null) { throw new BadRequestException("Metadata.key == null"); }
			if(m.value == null) { throw new BadRequestException("Metadata.value == null"); }

			obj.metadata_key = m.key;
			obj.metadata_value = m.value;

			if(m.created_by != null) { 
				obj.created_by = parseUserID(m.created_by.id);
			}

			if(m.created_on != null) { 
				try {
					obj.created_on = dateFormat.parse(m.created_on);
				} catch (ParseException e) {
					throw new BadRequestException(e.getMessage());
				}
			}

			list.add(obj);
		}

		return list;
	}
	
	private Request convertRequest(RequestObject reqObj) throws DBModelException, DBObjectMissingException { 
		DBObjectModel db = model.getModel();
		Request req = new Request();

		req.id = reqObj.provisional_term;
		req.ontology_term = reqObj.ontology_term;
		req.search_text = reqObj.search_text;
		req.context = reqObj.context;
		req.date_submitted = formatDate(reqObj.date_submitted);
		req.status = RequestStateServlet.STATES.backward(reqObj.status);
		req.ontology = loadOntology(reqObj.ontology_id);
		req.creator = loadUser(reqObj.creator_id);
		req.modified_by = loadUser(reqObj.modified_by);

		req.metadata = new ArrayList<Metadata>();
		MetadataObject metaTemplate = new MetadataObject();
		metaTemplate.request_id = reqObj.request_id;
		Collection<MetadataObject> metaList = db.load(MetadataObject.class, metaTemplate);
		
		for(MetadataObject metaObj : metaList) { 
			req.metadata.add(convertMetadata(metaObj));
		}
		return req;

	}
	
	public Request check(UserCredentials user, String request)
			throws BrokerException {
		
		try {
			ProvisionalTermObject termObj = model.getProvisionalTerm(request);
			RequestObject reqObj = model.getLatestRequest(termObj);
			
			return convertRequest(reqObj);

		} catch (DBModelException e) {
			throw new InternalServerErrorException(e);
		} catch (DBObjectMissingException e) {
			throw new GoneException(request);
		}
	}

	public void judge(UserCredentials user, String request, String judgement, String comment)
			throws BrokerException {
		
		try {
			judgement = judgement.toLowerCase();
			
			ProvisionalTermObject termObj = model.getProvisionalTerm(request);
			RequestObject reqObj = model.getLatestRequest(termObj);
			
			RequestObject newObj = new RequestObject();
			newObj.setFromDBObject(reqObj);
			
			if(judgement.equals("accept")) { 
				newObj.status = RequestStateServlet.STATES.forward("ACCEPTED");
				
			} else if (judgement.equals("reject")) {
				if(comment == null || comment.length() == 0) { 
					throw new BadRequestException("No comment given.");
				}
				
				newObj.status = RequestStateServlet.STATES.forward("PENDING");
			} else { 
				throw new BadRequestException("Illegal judgement " + judgement);
			}
			
			model.startTransaction();
			try { 
				model.updateRequest(termObj, newObj, loadMetadatas(newObj.request_id));
				model.commitTransaction();
			
			} catch(DBModelException e) { 
				model.rollbackTransaction();
				throw e;
			}

		} catch (DBModelException e) {
			throw new InternalServerErrorException(e);
		} catch (DBObjectMissingException e) {
			throw new GoneException(request);
		}		
	}

	public Ontology[] listOntologies(UserCredentials user)
			throws BrokerException {
		
		try {
			Collection<OntologyObject> onts = model.listOntologies();
			ArrayList<Ontology> list = new ArrayList<Ontology>();
			for(OntologyObject obj : onts) { 
				list.add(convertOntology(obj));
			}
			
			return list.toArray(new Ontology[0]);
			
		} catch (DBModelException e) {
			throw new InternalServerErrorException(e);
		} catch (DBObjectMissingException e) {
			throw new GoneException(e.getMessage());
		}
	}
	
	public Request[] listRequests(UserCredentials user, Ontology ontology) throws BrokerException { 
		try {
			Collection<RequestObject> reqObjs = model.listLatestRequests();
			Log.info(String.format("# Latest requests: %d", reqObjs.size()));
			
			ArrayList<Request> reqs = new ArrayList<Request>();
			
			for(RequestObject obj : reqObjs) {
				Request request = convertRequest(obj);
				if(ontology == null || request.ontology.equals(ontology)) { 
					reqs.add(request);
				} else { 
					Log.info(String.format("Request %s ontology %s != %s", 
							request.id,
							request.ontology.id,
							ontology.id));
				}
			}
			
			return reqs.toArray(new Request[0]);
			
		} catch (DBModelException e) {
			throw new BrokerException(e);
			
		} catch (DBObjectMissingException e) {
			throw new GoneException(e.getMessage());
		}
	}

	@Override
	public User[] listUsers(UserCredentials user, String ontology)
			throws BrokerException {
		try {
			Collection<UserObject> users = model.listUsers();
			ArrayList<User> list = new ArrayList<User>();
			for(UserObject obj : users) { 
				list.add(convertUser(obj));
			}
			
			return list.toArray(new User[0]);
			
		} catch (DBModelException e) {
			throw new InternalServerErrorException(e);
		}
	}

	public SearchResult[] query(UserCredentials user, String query,
			String... ontologyIds) throws BrokerException, GoneException {

		try { 
			ProteinSearcher searcher = new ProteinSearcher(new File(luceneIndexPath));
			
			Set<String> types = null;
			if(ontologyIds != null && ontologyIds.length > 0) { 
				types = new TreeSet<String>();
				for(String id : ontologyIds) { 
					Ontology ont = checkOntology(user, id);
					types.add(ont.ontology_name);
				}
			}
			
			try { 

				ArrayList<SearchResult> results = new ArrayList<SearchResult>();
				JSONArray hits = searcher.evaluate(query);
				
				for(int i = 0; i < hits.length(); i++) { 
					JSONObject obj = hits.getJSONObject(i);
					SearchResult result = new SearchResult(obj);
				
					if(types==null || types.contains(result.response_type)) { 
						results.add(result);
					}
				}
				
				return results.toArray(new SearchResult[0]);

			} finally { 
				searcher.close();
			}
			
		} 	catch (JSONException e) { throw new BrokerException(e); }
			catch(IOException e) { throw new BrokerException(e); }

	}

	public String request(UserCredentials user, 
		
			String name, 
			String context, 
			String provenance,
			
			Ontology ontology,
			
			Collection<Metadata> metadatas) throws BrokerException {
		
		RequestObject reqObj = new RequestObject();
		
		reqObj.search_text = name;
		reqObj.context = context;
		reqObj.provenance = provenance;
		reqObj.creator_id = user.getUserID();
		reqObj.modified_by = user.getUserID();
		reqObj.date_submitted = Calendar.getInstance().getTime();
		reqObj.ontology_id = parseOntologyID(ontology.id);
		reqObj.status = RequestStateServlet.STATES.forward("PENDING");
		
		Collection<MetadataObject> metaObjs = unconvertMetadata(metadatas);
		
		try {
			ProvisionalTermObject termObj = model.createNewRequest(reqObj, metaObjs);

			return termObj.provisional_term;
			
		} catch (DBModelException e) {
			throw new BrokerException(e);
		}
	}

	public BulkTable listRequestsInBulk(UserCredentials user, String ontology)
			throws BrokerException {

		Ontology ont = checkOntology(user, ontology);
		BulkTable table = new BulkTable(ont);
		
		Request[] reqs = listRequests(user, ont);
		Log.info(String.format("# Listed bulk requests: %d", reqs.length));
		
		for(Request req : reqs) { 
			table.addRequest(req);
		}
		
		return table;
	}
	
	public void respondInBulk(UserCredentials user, BulkTable response)
	throws BrokerException {

		try { 
			model.startTransaction();
			try { 
			
				for(int i = 0; i < response.lines.size(); i++) { 
					
					Request request = response.getRequest(i);
					RequestObject reqObj = new RequestObject();
					
					unconvertRequest(reqObj, request);
					
					Collection<MetadataObject> metaObjs = unconvertMetadata(request.metadata);
					
					ProvisionalTermObject termObj = model.getProvisionalTerm(request.id);
					
					model.updateRequest(termObj, reqObj, metaObjs);
				}
				

			} catch(DBModelException e) { 
				model.rollbackTransaction();
				throw e;
			} catch (DBObjectMissingException e) {
				model.rollbackTransaction();
				throw e;
			}

		} catch (DBModelException e) {
			throw new BrokerException(e);
		} catch(DBObjectMissingException e) { 
			throw new BrokerException(e);
		}
	}

	/**
	 * @inheritDocs
	 */
	public void update(UserCredentials user, String request, Request requestData)
			throws BrokerException {
		
		RequestObject reqObject = new RequestObject();
		unconvertRequest(reqObject, requestData);
		Collection<MetadataObject> metaObjs = unconvertMetadata(requestData.metadata);
		
		try {
			ProvisionalTermObject termObj = model.getProvisionalTerm(request);
			model.updateRequest(termObj, reqObject, metaObjs);

		} catch (DBModelException e) {
			throw new BrokerException(e);
			
		} catch (DBObjectMissingException e) {
			throw new GoneException(e.getMessage());
		}
	}

	/**
	 * @throws BrokerException 
	 * @inheritDocs
	 */
	public void addOntologyField(UserCredentials user, Ontology ont, OntologyField newField) throws BrokerException {
		int ontologyID = parseOntologyID(ont.id);
		
		OntologyFieldObject obj = new OntologyFieldObject();
		obj.field_description = newField.description;
		obj.field_name = newField.name;
		obj.field_metadata_key = newField.metadata_key;
		obj.ontology_id = ontologyID;
		
		try {
			model.createNewOntologyField(obj);
		} catch (DBModelException e) {
			throw new BrokerException(e);
		}
	}
}
