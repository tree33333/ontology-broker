package org.sc.probro.servlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.eclipse.jetty.util.log.Log;
import org.sc.obo.*;
import org.sc.probro.Broker;
import org.sc.probro.BrokerProperties;
import org.sc.probro.Ontology;
import org.sc.probro.UserCredentials;
import org.sc.probro.data.BrokerModel;
import org.sc.probro.data.DBModelException;
import org.sc.probro.exceptions.BadRequestException;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.lucene.IndexCreator;
import org.sc.probro.sparql.BindingTable;
import org.sc.probro.sparql.OBOBuilder;
import org.sc.probro.sparql.OBOSparql;
import org.sc.probro.sparql.Prefixes;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class IndexCreatorServlet extends BrokerServlet {

	private OBOSparql oboSparql;
	
	public IndexCreatorServlet(BrokerProperties ps) {
		super(ps);
		oboSparql = new OBOSparql(ps);
	}
	
	public void init() throws ServletException { 
		super.init();
	}
	
	public void destroy() { 
		super.destroy();
	}
	
	public static final int MB = 1024*1024;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			OBOBuilder builder = new OBOBuilder(oboSparql);
			String oboName = getRequiredParam(request, "obo", String.class);
			
			try { 
				String oboText = builder.loadOBO(oboName);

				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text");
				PrintWriter writer = response.getWriter();
				writer.println(oboText);

			} catch(IOException e) { 
				throw new BrokerException(e);
			}
			
		} catch (BrokerException e) {
			handleException(response, e);
			return;
		}
	}

	public static DiskFileItemFactory newDiskFileItemFactory(ServletContext context, File repository) {
		
		FileCleaningTracker fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(context);

		DiskFileItemFactory factory = new DiskFileItemFactory(10*MB, repository);
		factory.setFileCleaningTracker(fileCleaningTracker);
	
		return factory;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			OBOParser parser = null;
			Map<String,String[]> params = decodedParams(request);

			UserCredentials creds = new UserCredentials();
			
			String ontologyName = null;
			
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			if(!isMultipart) { 
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, "No multipart form data.");
			}
			
			try {
				File repository = new File(System.getProperty("java.io.tmpdir"));

				// Create a factory for disk-based file items
				//DiskFileItemFactory factory = new DiskFileItemFactory();
				DiskFileItemFactory factory = newDiskFileItemFactory(getServletContext(), repository);
				
				// Set factory constraints
				factory.setSizeThreshold(10*MB);
				//factory.setRepository(yourTempDirectory);

				// Create a new file upload handler
				ServletFileUpload upload = new ServletFileUpload(factory);
				upload.setSizeMax(50*MB);

				// Parse the request
				List<FileItem> items = upload.parseRequest(request);
				
				for(FileItem item : items) { 
					if(item.isFormField()) { 
						String formName = item.getFieldName();
						String formValue = item.getString();
						Log.info(String.format("%s=%s", formName, formValue));
						
						if(formName.equals("ontology_name")) { 
							ontologyName = formValue;
						}

					} else {
						String formName = item.getFieldName();
						String fileName = item.getName();
						String contentType = item.getContentType();
						boolean isInMemory = item.isInMemory();
						long sizeInBytes = item.getSize();

						if(formName.equals("ontology_file")) {
							
							Log.info(String.format("fileName=%s, contentType=%s, size=%d", fileName, contentType, sizeInBytes));
							
							if(fileName.length() > 0) { 

								InputStream uploadedStream = item.getInputStream();
								BufferedReader reader = new BufferedReader(new InputStreamReader(uploadedStream));

								parser = new OBOParser();
								parser.parse(reader);

								reader.close();
							}
							
						} else { 
							Log.warn(String.format("unknown file: %s", formName));
						}

					}
				}
			} catch(IOException e) { 
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			} catch (FileUploadException e) {
				throw new BrokerException(e);
			}
			
			if(ontologyName == null) { 
				throw new BadRequestException("No ontology_name field given.");
			}
			
			
			OBOOntology ontology = null; 
				
			try {
				if(parser != null) { 
					Log.info(String.format("Retrieving OBO ontology from file."));
					// file was uploaded
					ontology = parser.getOntology();
				} else { 
					// try to get it from teh sparql endpoint
					Log.info(String.format("No OBO file uploaded, reading from Sparql endpoint instead."));
					
					OBOBuilder builder = new OBOBuilder(oboSparql);
					ontology = builder.buildOntology(ontologyName);
				}
			} catch(IOException e) { 
				throw new BrokerException(e);
			}
			
			Broker broker = getBroker();
			try {
				Ontology ont = broker.createOntology(creds, ontologyName, ontology);
				
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text");
				response.getWriter().print(ont.id);
				
			} finally { 
				broker.close();
			}
			
		} catch(BrokerException e) { 		
			handleException(response, e);
			return;			
		}
	}

}
