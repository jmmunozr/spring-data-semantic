package org.springframework.data.semantic.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the functionality for safe creation of repositories i.e.
 * the Repository management tasks are handled by RepositoryManagers which are never 
 * duplicated for a certain location.
 * 
 * @author petar.kostov
 *
 */
public class SemanticDatabaseManager{
	
	private SemanticDatabaseManager() {}

	//----Member variables-----------------------------------------------------
	
	private static final String DEFAULT_CONFIG_FILE = "META-INF/config/data-memory.ttl";
	
	private static Map<String, RepositoryManager> openRepositoryManagers = new HashMap<String, RepositoryManager>();
	private static Logger log = LoggerFactory.getLogger(SemanticDatabaseManager.class);
	
	
	//----Public methods-------------------------------------------------------
	
	/**
	 * Gets a local/remote repository. If it does not exist, creates a new repository with the specified configuration.
	 * If no configuration is supplied, a default one is used.
	 * @param baseURL the base location for the RepositoryManager
	 * @param repoId the ID of the repository in question. When creating a new repo, this parameter overrides the Id specified in the config paramer
	 * @param username for authentication against a remote repository
	 * @param password for authentication against a remote repository
	 * @param config configuration used when creating a new repository
	 * @return the initialized repository
	 */
	public static Repository getRepository(String baseURL, String repoId, 
			String username, String password, RepositoryConfig config) {

		Repository repo = null;
		try {
			//get an existing repositorymanager or create a new one
			RepositoryManager manager = getRepositoryManagerForLocation(baseURL, username, password);

			if((repo = manager.getRepository(repoId)) != null) {
				repo.initialize();
			} else{
				if(baseURL.startsWith("http")) {					
					throw new UnsupportedOperationException("Repository creation for remote locations is not currently supported.");
				}
				repo = createRepository(manager, repoId, config);	
			}			
			
		} catch(RepositoryException re) {
			log.error("Error opening repository for location {}", baseURL, re);
		} catch(RepositoryConfigException rce) {
			log.error("Error opening repository for location {}", baseURL, rce);
		}
		
		return repo;
	}
	
	/**
	 *see getRepository(String baseURL, String repoId, String username, String password, RepositoryConfig config)
	 */
	public static Repository getRepository(String baseURL, String repoId, RepositoryConfig config) {
		return getRepository(baseURL, repoId, "", "", config);
	}
	
	/**
	 *see getRepository(String baseURL, String repoId, String username, String password, RepositoryConfig config)
	 */
	public static Repository getRepository(String baseURL, String repoId) {
		return getRepository(baseURL, repoId, "", "", null);
	}

	public static Repository getRepository(String repoURL, RepositoryConfig config) {
		String[] parts = parseRepositoryURL(repoURL);
		return getRepository(parts[0],  parts[1], config);
	}
	
	public static Repository getRepository(String repoURL, String username, String password, RepositoryConfig config){
		String[] parts = parseRepositoryURL(repoURL);
		return getRepository(parts[0],  parts[1], username, password, config);
	}
	
	public static Repository getRepository(String repoURL) {
		String[] parts = parseRepositoryURL(repoURL);
		return getRepository(parts[0],  parts[1]);
	}
		
	public static synchronized void shutdownManager(String baseURL) {
		
		RepositoryManager manager = openRepositoryManagers.get(baseURL);
		if(manager == null)
			return;
		manager.shutDown();
		openRepositoryManagers.remove(baseURL);
	}
	
	public static RepositoryConfig getDefaultConfig() throws RepositoryConfigException, 
	RDFParseException, RDFHandlerException, IOException, GraphUtilException {
		
		RepositoryConfig defaultConfig = new RepositoryConfig();
		Graph graph = new GraphImpl();

		InputStream configStream = SemanticDatabaseManager.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		rdfParser.parse(configStream, RepositoryConfigSchema.NAMESPACE);			

		Resource repositoryNode = GraphUtil.getUniqueSubject(graph, RDF.TYPE, RepositoryConfigSchema.REPOSITORY);

		defaultConfig.parse(graph, repositoryNode);
		return defaultConfig;
	}
	
	
	//----Private methods------------------------------------------------------

	private static Repository createRepository(RepositoryManager manager, String repositoryId, RepositoryConfig config) 
			throws RepositoryException,	RepositoryConfigException {
		
		Repository repo;
		if(config == null) {
			log.info("Config is null, opening a repository with default config.");
			try {
				config = getDefaultConfig();
			} catch (Exception e) {
				log.error("Cannot load default config. ", e);
				return null;
			}
		}
		
		//use the explicit repository Id if supplied
		if(!(repositoryId == null || repositoryId.isEmpty())) {
			config.setID(repositoryId);
		}
		
		manager.addRepositoryConfig(config);
		repo = manager.getRepository(config.getID());
		repo.initialize();
		return repo;
	}
	
	private static synchronized RepositoryManager getRepositoryManagerForLocation(String serverURL, 
			String username, String password) throws RepositoryException {
		
		RepositoryManager repoManager;
		if((repoManager = openRepositoryManagers.get(serverURL)) != null) {
			log.info("Repository manager found for base location {}.", serverURL);
			return repoManager;
		}
		
		if(serverURL.startsWith("http")) {
			repoManager = createNewRemoteRepositoryManager(serverURL, username, password);
		} else{
			repoManager = createNewLocalRepositoryManager(serverURL);
		}
		
		if(repoManager != null) {
			//TODO absolute/relative paths
			openRepositoryManagers.put(serverURL, repoManager);
		}
		
		return repoManager;		
	}

	private static RepositoryManager createNewLocalRepositoryManager(
			String baseDir) throws RepositoryException {
		
		log.info("Creating new local repository manager for base location {}.", baseDir);
		LocalRepositoryManager repoManager = new LocalRepositoryManager(new File(baseDir));
		repoManager.initialize();
		return repoManager;
	}

	private static RepositoryManager createNewRemoteRepositoryManager(
			String serverURL, String username, String password) throws RepositoryException {
		
		log.info("Creating new remote repository manager for base location {}.", serverURL);
		RemoteRepositoryManager repoManager = new RemoteRepositoryManager(serverURL);
		repoManager.setUsernameAndPassword(username, password);
		repoManager.initialize();
		return repoManager;		
	}
	
	private static String[] parseRepositoryURL(String repoURL) {
		String[] parts = repoURL.split("/"+Protocol.REPOSITORIES+"/");
		if(parts.length != 2)
			throw new InvalidParameterException("The repository URL should be in the form: <base-url>/"+Protocol.REPOSITORIES+"/<repo-id>");
		return parts;
	}
}