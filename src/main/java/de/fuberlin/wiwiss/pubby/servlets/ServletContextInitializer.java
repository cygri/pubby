package de.fuberlin.wiwiss.pubby.servlets;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.pubby.Configuration;

public class ServletContextInitializer implements ServletContextListener {
	public final static String SERVER_CONFIGURATION =
			ServletContextInitializer.class.getName() + ".serverConfiguration";
	public final static String ERROR_MESSAGE =
			ServletContextInitializer.class.getName() + ".errorMessage";
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		String configFileName = context.getInitParameter("config-file");
		if (configFileName == null) {
			throw new RuntimeException("Missing context parameter 'config-file' in web.xml");
		}
		File configFile = new File(configFileName);
		if (!configFile.isAbsolute()) {
			configFile = new File(context.getRealPath("/") + "/WEB-INF/" + configFileName);
		}
		try {
			Model m = FileManager.get().loadModel(
					configFile.getAbsoluteFile().toURI().toString());
			Configuration conf = Configuration.create(m);
			context.setAttribute(SERVER_CONFIGURATION, conf);
		} catch (Exception ex) {
			System.out.println(ex);
			context.setAttribute(ERROR_MESSAGE, ex.getMessage());
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Do nothing special.
	}
}
