package org.sc.probro;

import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.Servlet;

import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.log.Log;
import org.sc.probro.data.*;
import org.sc.probro.servlets.*;

public class BrokerStart {
	
	public static String HOSTNAME = "localhost";
	public static int PORT = 8080;
	
	static { 
		try {
			HOSTNAME = java.net.Inet4Address.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			System.err.println(String.format("Unable to find HOSTNAME: %s", e.getMessage()));
		}
	}

	public static void main(String[] args) throws Exception {
		BrokerProperties props = new BrokerProperties();
		
		int port = args.length > 0 ? Integer.parseInt(args[0]) : props.getPort();
		PORT = port;
		
		String resourceBase = props.getResourceBase();
		
		BrokerStart js = new BrokerStart(port, resourceBase);
		
		//js.addServlet("jsp", new JspServlet(), "*.jsp");

		js.addServlet("User", new DBObjectServlet<User>(props, User.class, "user_id"), "/user/*");
		js.addServlet("Request", new RequestServlet(props), "/request/*");		
		js.addServlet("Ontology", new DBObjectServlet<Ontology>(props, Ontology.class, "ontology_id"), "/ontology/*");		
		js.addServlet("BulkRequests", new BulkRequestServlet(props), "/bulk-requests");
		js.addServlet("Requests", new RequestListServlet(props), "/requests");
		js.addServlet("Users", new DBObjectListServlet<User>(props, User.class), "/users");
		//js.addServlet("Ontologies", new DBObjectListServlet<Ontology>(props, Ontology.class), "/ontologies");
		js.addServlet("Ontologies", new OntologyListServlet(props), "/ontologies");
		js.addServlet("TextQuery", new TextQueryServlet(props), "/query");
		js.addServlet("States", new RequestStateServlet(), "/states");
		js.addServlet("Proteins", new BiothesaurusQueryServlet(props), "/proteins/*");

		//js.addServlet("Metadatas", new DBObjectListServlet<Metadata>(props, Metadata.class), "/metadata");
		//js.addServlet("Metadata", new DBObjectServlet<Metadata>(props, Metadata.class, "metadata_id"), "/metadatum/*");

		//js.addServlet("IndexCreator", new IndexCreatorServlet(props), "/indexer/*");
		
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownRunnable(js)));
		js.start();
	}
	
	private static class ShutdownRunnable implements Runnable { 
		private BrokerStart broker;
		
		public ShutdownRunnable(BrokerStart bs) { 
			broker = bs;
		}
		
		public void run() { 
			System.out.println("Shutdown hook running..."); System.out.flush();
			broker.stop();
		}
	}
	
	private Server server;
	private ServletContextHandler context;
	
	public BrokerStart(int port, String resourceBase) { 
		server = new Server(port);
        Map<String,String> params = new TreeMap<String,String>();
        params.put("org.apache.jasper.Constants.SERVLET_CLASSPATH", "org.sc.probro.jsps");
        params.put("org.apache.jasper.servlet.JspServlet.classpath", "org.sc.probro.jsps");
                
        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(resourceBase);
        context.setInitParams(params);
        
        // This is the reaper thread for the Apache FileUpload utility, which cleans out 
        // the temporary files which have been uploaded.
        // See 
        // http://commons.apache.org/fileupload/using.html
        // for more details.
        context.addEventListener(new FileCleanerCleanup());
        
        ResourceHandler recs = new ResourceHandler();
        recs.setResourceBase(resourceBase + "/static");
        recs.setCacheControl("max-age=3600");
        
        RequestLogHandler logHandler = new RequestLogHandler();
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context, recs  });
        
        logHandler.setHandler(handlers);
        
        server.setHandler(logHandler);
        
        //server.setHandler(handlers);
        
        // Taken from example: 
        // http://wiki.eclipse.org/Jetty/Tutorial/RequestLog
        NCSARequestLog requestLog = new NCSARequestLog("./logs/broker-yyyy_mm_dd.request.log");
        requestLog.setRetainDays(90);
        requestLog.setAppend(true);
        requestLog.setExtended(false);
        requestLog.setLogTimeZone("GMT");
        logHandler.setRequestLog(requestLog);
        
	}
	
	public void addServlet(String servletName, Servlet servlet, String firstMapping, String... mappings) { 
		ServletHolder holder = new ServletHolder(servlet);
		holder.setName(servletName);
	
		context.addServlet(holder, firstMapping);
		
		if(mappings.length > 0) { 
			ServletMapping servletMapping = new ServletMapping();
			servletMapping.setServletName(servletName);
			servletMapping.setPathSpecs(mappings);
			context.getServletHandler().addServletMapping(servletMapping);
		}
	}
	
	public void start() throws Exception { 
		server.start();
		server.join();
	}
	
	public void stop() { 
		try {
			if(server.isStarted()) { 
				server.stop();
			} else { 
				System.out.println("Ignoring stop on unstarted server.");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}