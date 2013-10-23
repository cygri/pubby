package de.fuberlin.wiwiss.pubby.servlets;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.ConfigurationException;

public class ServletContextInitializer implements ServletContextListener {
	public final static String SERVER_CONFIGURATION =
			ServletContextInitializer.class.getName() + ".serverConfiguration";
	public final static String ERROR_MESSAGE =
			ServletContextInitializer.class.getName() + ".errorMessage";
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		try {
			String configFileName = context.getInitParameter("config-file");
			if (configFileName == null) {
				throw new ConfigurationException("Missing context parameter \"config-file\" in /WEB-INF/web.xml");
			}
			File configFile = new File(configFileName);
			if (!configFile.isAbsolute()) {
				configFile = new File(context.getRealPath("/") + "/WEB-INF/" + configFileName);
			}
			String url = configFile.getAbsoluteFile().toURI().toString();
			try {
				Model m = FileManager.get().loadModel(url);
				Configuration conf = Configuration.create(m);
				context.setAttribute(SERVER_CONFIGURATION, conf);
			} catch (JenaException ex) {
				throw new ConfigurationException(
						"Error parsing configuration file <" + url + ">: " + 
						ex.getMessage());
			}
		} catch (ConfigurationException ex) {
			log(ex, context);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Do nothing special.
	}
	
	private void log(Exception ex, ServletContext context) {
		context.log("######## PUBBY CONFIGURATION ERROR ######## ");
		context.log(ex.getMessage());
		context.log("########################################### ");
		context.setAttribute(ERROR_MESSAGE, ex.getMessage());
	}
}
