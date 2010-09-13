package org.sc.probro.data;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.sc.probro.BrokerException;
import org.sc.probro.BrokerStart;
import org.sc.probro.servlets.RequestStateServlet;

public class BrokerModel {

	private DBObjectModel model;

	public BrokerModel(DBObjectModel objModel) { 
		model = objModel;
	}
	
	public synchronized boolean contains(Ontology ontology) throws DBModelException { 
		return model.count(Ontology.class, ontology) > 0;
	}
	
	public synchronized boolean contains(User user) throws DBModelException { 
		return model.count(User.class, user) > 0;
	}
	
	public synchronized boolean contains(ProvisionalTerm term) throws DBModelException { 
		return model.count(ProvisionalTerm.class, term) > 0;
	}
	
	public synchronized String checkRequestChange(Request oldReq, Request newReq) {

		if(!oldReq.creator_id.equals(newReq.creator_id)) { 
			return String.format("Illegal CREATOR_ID change (%d -> %d)", 
					oldReq.creator_id, newReq.creator_id);
		}
		
		assert oldReq != null : "null oldRequest";
		assert newReq != null : "null newRequest";
		assert oldReq.status != null : "Null oldRequest.status";
		
		if(newReq.status == null) { 
			return String.format("Illegal new request status: null");
		}
		
		String oldStatus = RequestStateServlet.STATES.backward(oldReq.status);
		String newStatus = RequestStateServlet.STATES.backward(newReq.status);
		
		if(!oldStatus.equals(newStatus) && !RequestStateServlet.MACHINE.isReachable(oldStatus,newStatus)) { 
			return String.format("Illegal Request STATUS transition: %s -> %s", oldStatus, newStatus);
		}
		
		return null;
	}
	
	public static String md5(Integer id) { 
		return md5(String.format("%010d", id));
	}
	
	public static String md5(String s) {
		try { 
			MessageDigest m=MessageDigest.getInstance("MD5");
			m.update(s.getBytes(),0,s.length());
			byte[] bytes = m.digest();
			BigInteger integer = new BigInteger(1, bytes);
			return integer.toString(16);
			
		} catch(NoSuchAlgorithmException e) { 
			throw new IllegalStateException("No MD5 hash available.");
		}
	}
	
	public synchronized String generateRandomTerm() { 
		Random rand = new Random();
		int randSeed = rand.nextInt();
		return md5(randSeed);
	}

	/*
	public synchronized String generateNewProvisionalTerm(Request req) { 
		return String.format("http://%s:%d/request/%d/", 
				BrokerStart.HOSTNAME,
				BrokerStart.PORT,
				req.request_id);
	}
	*/
	
	public synchronized void close() throws DBModelException { 
		model.close();
	}
	
	public synchronized Collection<User> listUsers() throws DBModelException { 
		return model.load(User.class, new User());
	}
	
	public synchronized Collection<Ontology> listOntologies() throws DBModelException { 
		return model.load(Ontology.class, new Ontology());
	}
	
	public synchronized Collection<Request> listLatestRequests() throws DBModelException {
		LinkedList<Request> reqs = new LinkedList<Request>();

		// 
		// This is annoying -- I have to break the abstraction barrier here, if I want to avoid
		// a separate JDBC call for *each* request in the DB.
		// I don't want to take the time to design an additional interface to the DBObjectModel 
		// that can support this, so I go ahead and write the custom query for each supported
		// model type.
		// 
		if(model instanceof DatabaseDBObjectModel) { 
			DatabaseDBObjectModel db = (DatabaseDBObjectModel)model;
			Connection cxn = db.getConnection();
			
			try { 
				Statement stmt = cxn.createStatement();
				try { 
					ResultSet rs = stmt.executeQuery(
							"select * from provisionalterms, requests " +
							"where provisionalterms.request_id=requests.request_id " +
							"order by requests.date_submitted asc");
					try { 
						while(rs.next()) { 
							Request req = new Request(rs);
							reqs.add(req);
						}
						
					} finally { 
						rs.close();
					}
				} finally { 
					stmt.close();
				}
				
			} catch(SQLException e) { 
				throw new DBModelException(e);
			}
			
			// don't need to close the cxn at the end -- it's still "owned" by the DBObjectModel.
			
		} else { 
			throw new UnsupportedOperationException(
					String.format("listLatestRequests() not supported with DBObjectModel %s", 
							model.getClass().getSimpleName()));
		}
		
		return reqs;
	}
	
	public synchronized ProvisionalTerm getProvisionalTerm(final String term) throws DBModelException, DBObjectMissingException {
		ProvisionalTerm template = new ProvisionalTerm();
		template.provisional_term = term;
		return model.loadOnly(ProvisionalTerm.class, template);
	}
	
	public synchronized Collection<ProvisionalTerm> listProvisionalTerms() throws DBModelException { 
		return model.load(ProvisionalTerm.class, new ProvisionalTerm());
	}
	
	public synchronized Request getRequest(int request_id) throws DBModelException, DBObjectMissingException { 
		Request template = new Request();
		template.request_id = request_id;
		return model.loadOnly(Request.class, template);
	}
	
	public synchronized Request getLatestRequest(ProvisionalTerm term) throws DBModelException { 
		Request template = new Request();
		template.request_id = term.request_id;
		try {
			return model.loadOnly(Request.class, template);
		} catch (DBObjectMissingException e) {
			throw new DBModelException(String.format(
					"ProvisionalTerm request_id=%d doesn't match any known Request.", 
					template.request_id));
		}
	}
	
	public synchronized Collection<Request> getAllRequests(final ProvisionalTerm term) throws DBModelException {
		Request template = new Request(); 
		template.request_id = term.request_id;
		return model.load(Request.class, template, 
				"date_submitted DESC", null);
	}
	
	public synchronized Collection<Metadata> getMetadata(final Request req) throws DBModelException {
		Metadata template = new Metadata();
		template.request_id = req.request_id;
		return model.load(Metadata.class, template);
	}
	
	public synchronized void startTransaction() throws DBModelException { 
		model.startTransaction();
	}
	
	public synchronized void commitTransaction() throws DBModelException { 
		model.commitTransaction();
	}
	
	public synchronized void rollbackTransaction() throws DBModelException { 
		model.rollbackTransaction();
	}

	public synchronized ProvisionalTerm createNewRequest(Request req, Collection<Metadata> metadatas) throws DBModelException { 
		ProvisionalTerm term = new ProvisionalTerm();
		term.provisional_term = generateRandomTerm();
		model.create(ProvisionalTerm.class, term);

		req.provisional_term = term.provisional_term;
		model.create(Request.class, req);

		for(Metadata md : metadatas) { 
			md.request_id = req.request_id;
			md.created_on = req.date_submitted;
			md.created_by = req.modified_by;
			
			model.create(Metadata.class, md);
		}

		term.request_id = req.request_id;
		model.update(term);

		return term;
	}

	public synchronized void updateRequest(ProvisionalTerm term, Request req, Collection<Metadata> metadatas) throws DBModelException, BrokerException { 
		Request latest = getLatestRequest(term);

		String error = checkRequestChange(latest, req);
		if(error != null) { 
			throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, error);
		}

		req.parent_request = latest.request_id;

		model.create(Request.class, req);

		for(Metadata md : metadatas) { 
			md.request_id = req.request_id;
			md.created_on = req.date_submitted;
			md.created_by = req.modified_by;
			
			model.create(Metadata.class, md);
		}

		term.request_id = req.request_id;
		model.update(term);
	}
}
