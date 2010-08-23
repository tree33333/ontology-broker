package org.sc.probro;

import java.util.*;
import java.util.regex.Pattern;

import javax.servlet.Servlet;

import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.sc.probro.data.*;
import org.sc.probro.servlets.*;

public class BrokerStart {

	public static void main(String[] args) throws Exception {
		BrokerProperties props = new BrokerProperties();
		int port = args.length > 0 ? Integer.parseInt(args[0]) : props.getPort();
		String resourceBase = props.getResourceBase();
		
		BrokerStart js = new BrokerStart(port, resourceBase);
		
		//js.addServlet("jsp", new JspServlet(), "*.jsp");

		js.addServlet("User", new DBObjectServlet<User>(props, User.class, "user_id"), "/user/*");
		js.addServlet("Request", new RequestServlet(props), "/request/*");		
		js.addServlet("Ontology", new DBObjectServlet<Ontology>(props, Ontology.class, "ontology_id"), "/ontology/*");		
		js.addServlet("BulkRequests", new BulkRequestServlet(props), "/bulk-requests");
		js.addServlet("Requests", new RequestListServlet(props), "/requests");
		js.addServlet("Users", new DBObjectListServlet<User>(props, User.class), "/users");
		js.addServlet("Ontologies", new DBObjectListServlet<Ontology>(props, Ontology.class), "/ontologies");
		js.addServlet("Metadata", new DBObjectListServlet<Metadata>(props, Metadata.class), "/metadata");
		js.addServlet("TextQuery", new TextQueryServlet(props), "/query");
		
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
        
        ResourceHandler recs = new ResourceHandler();
        recs.setResourceBase(resourceBase + "/static");
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context, recs });
        
        server.setHandler(handlers);
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