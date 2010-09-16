package org.sc.probro;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.*;

import org.sc.probro.data.*;
import org.sc.probro.exceptions.*;
import org.sc.probro.servlets.RequestStateServlet;

public class LocalBroker implements Broker {
	
	private String urlPrefix;
	private BrokerModel model;
	
	public LocalBroker(String uprefix, BrokerModel m) {
		urlPrefix = uprefix;
		if(urlPrefix.endsWith("/")) { urlPrefix = urlPrefix.substring(0, urlPrefix.length()-1); }
		model = m;
	}
	
	public String url(String path) { 
		if(path.startsWith("/")) { 
			return urlPrefix + path;
		} else { 
			return urlPrefix + "/" + path;
		}
	}
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	public String formatDate(Date d) { 
		return dateFormat.format(d);
	}
	
	public String ontologyURL(int id) { 
		return url(String.format("/ontology/%d/", id));
	}
	
	public String userURL(int id) { 
		return url(String.format("/user/%d/", id));
	}
	
	public Integer parseUserID(String url) { 
		Pattern p = Pattern.compile("^.*/user/([^\\/]+)/.*$");
		Matcher m = p.matcher(url);
		return m.matches() ? Integer.parseInt(m.group(1)) : null;
	}
	
	private Ontology loadOntology(int ontology_id) throws DBModelException, DBObjectMissingException { 
		DBObjectModel db = model.getModel();
		OntologyObject template = new OntologyObject();
		template.ontology_id = ontology_id;
		return convertOntology(db.loadOnly(OntologyObject.class, template));
	}
	
	private Ontology convertOntology(OntologyObject ontObj) { 
		Ontology ont = new Ontology();
		ont.id = ontologyURL(ontObj.ontology_id);
		ont.name = ontObj.name;
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
	
	private void unconvertRequest(RequestObject base, Request req) throws BadRequestException {
		
		base.ontology_term = req.ontology_term;
		base.search_text = req.search_text;
		base.context = req.context;
		base.comment = req.comment;
		try {
			base.date_submitted = dateFormat.parse(req.date_submitted);
		} catch (ParseException e) {
			throw new BadRequestException(e.getMessage());
		}
	
		base.status = RequestStateServlet.STATES.forward(req.status);
		base.modified_by = parseUserID(req.modified_by.id);
	}
	
	private Collection<MetadataObject> unconvertMetadata(Collection<Metadata> metas) throws BadRequestException { 
		LinkedList<MetadataObject> list = new LinkedList<MetadataObject>();
		
		for(Metadata m : metas) { 
			MetadataObject obj = new MetadataObject();

			obj.metadata_key = m.key;
			obj.metadata_value = m.value;
			obj.created_by = parseUserID(m.created_by.id);
			
			try {
				obj.created_on = dateFormat.parse(m.created_on);
			} catch (ParseException e) {
				throw new BadRequestException(e.getMessage());
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
			model.updateRequest(termObj, newObj, loadMetadatas(newObj.request_id));
			model.commitTransaction();

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
		}
	}

	@Override
	public BulkRequestTable listRequests(UserCredentials user, String ontology)
			throws BrokerException {
		
		// TODO : fix me.
		throw new UnsupportedOperationException("listRequests");
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

	public SearchResult[] query(UserCredentials user, String search,
			String... ontologyId) throws BrokerException {
	
		// TODO : fix me.
		// this is handled (at the moment) directly in the Servlet.
		throw new UnsupportedOperationException("query");
	}

	public String request(UserCredentials user, 
			String name, 
			String context, 
			String provenance, Collection<Metadata> metadatas) throws BrokerException {
		
		RequestObject reqObj = new RequestObject();
		Collection<MetadataObject> metaObjs = unconvertMetadata(metadatas);
		
		try {
			ProvisionalTermObject termObj = model.createNewRequest(reqObj, metaObjs);
			
			return termObj.provisional_term;
			
		} catch (DBModelException e) {
			throw new BrokerException(e);
		}
	}

	public void respond(UserCredentials user, BulkResponseTable response)
			throws BrokerException {

		// TODO: fix me.
		throw new UnsupportedOperationException("respond");
	}

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

}
