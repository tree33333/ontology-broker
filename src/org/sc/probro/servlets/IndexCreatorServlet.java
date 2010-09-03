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
import org.sc.probro.BrokerException;
import org.sc.probro.BrokerProperties;
import org.sc.probro.lucene.IndexCreator;
import org.sc.probro.sparql.BindingTable;
import org.sc.probro.sparql.OBOSparql;
import org.sc.probro.sparql.Prefixes;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class IndexCreatorServlet extends SkeletonServlet {

	private File indexDir;
	private OBOSparql oboSparql;
	
	public IndexCreatorServlet(BrokerProperties ps) { 
		oboSparql = new OBOSparql(ps);
		indexDir = new File(ps.getLuceneIndex());
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
			String oboName = getRequiredParam(request, "obo", String.class);
			String oboURI = OBOSparql.createGraphURI(oboName);
			
			if(oboURI.indexOf(">") != -1) { 
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, 
						String.format("Illegal OBO Name: %s", oboName));
			}
			
			Prefixes prefs = Prefixes.DEFAULT;
			
			String query = String.format(
					"%s\nselect count(?s) where { graph <%s> { ?s a owl:Class . } }", 
					prefs.getSparqlPrefixStatement("owl"), oboURI);
			
			BindingTable<RDFNode> tbl = oboSparql.query(query);
			Literal countLiteral = tbl.get(0).as(Literal.class);
			int count = countLiteral.getInt();
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text");
			PrintWriter writer = response.getWriter();
			writer.println(String.format("%s\n# Classes: %d", oboURI, count));
			
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
	
	public void addOntologyToIndex(OBOOntology ontology) throws BrokerException {
		
		Collection<OBOStanza> stanzas = ontology != null ? ontology.getStanzas() : 
			new LinkedList<OBOStanza>();

		File dir = indexDir;
		String type = "ontology";

		try { 
			IndexCreator creator = new IndexCreator(dir);
			try { 
				for(OBOStanza stanza : stanzas) { 
					String id = stanza.id();
					
					Set<String> accs = new TreeSet<String>();
					Set<String> descs = new TreeSet<String>();
					
					List<OBOValue> values = stanza.values("synonym");
					Pattern p = Pattern.compile("\"(.*)\" (EXACT)|(RELATED) \\[\\]");
					
					for(OBOValue value : values) { 
						Matcher m = p.matcher(value.getValue());
						if(m.matches()) { 
							accs.add(m.group(1));
						} else { 
							accs.add(value.getValue());
						}
					}
					
					descs.add(stanza.values("name").get(0).getValue());
					descs.add(stanza.values("def").get(0).getValue());
					
					for(OBOValue value : stanza.values("xref")) { 
						accs.add(value.getValue());
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

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			OBOParser parser = new OBOParser();
			Map<String,String[]> params = decodedParams(request);
			
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

					} else {
						String formName = item.getFieldName();
						String fileName = item.getName();
						String contentType = item.getContentType();
						boolean isInMemory = item.isInMemory();
						long sizeInBytes = item.getSize();

						if(formName.equals("ontology_file")) {
							
							Log.info(String.format("fileName=%s, contentType=%s, size=%d", fileName, contentType, sizeInBytes));

							InputStream uploadedStream = item.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(uploadedStream));

							/*
							String line; 
							System.out.println("Ontology: ");
							while((line = reader.readLine()) != null) { 
								System.out.println(line);
							}
							System.out.println("Done.");
							*/
							
							parser.parse(reader);

							reader.close();
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
			
			OBOOntology ontology = parser.getOntology();
			addOntologyToIndex(ontology);
			
		} catch(BrokerException e) { 		
			handleException(response, e);
			return;
		}
	}

}
