package org.springframework.data.semantic.support;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.semantic.core.SemanticDatabase;
import org.springframework.data.semantic.core.SemanticOperationsStatementsCollector;
import org.springframework.data.semantic.core.StatementsIterator;
import org.springframework.data.semantic.mapping.MappingPolicy;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;
import org.springframework.data.semantic.mapping.SemanticPersistentProperty;
import org.springframework.data.semantic.support.mapping.SemanticMappingContext;
import org.springframework.data.semantic.support.mapping.SemanticPersistentEntityImpl;

public class SemanticTemplateStatementsCollector implements SemanticOperationsStatementsCollector {
	
	private SemanticDatabase semanticDB;	
	private SemanticMappingContext mappingContext;
	
	public SemanticTemplateStatementsCollector(SemanticDatabase semanticDB, ConversionService conversionService, 
			SemanticMappingContext mappingContext) throws RepositoryException {
		
		this.semanticDB = semanticDB;
		this.mappingContext = mappingContext != null ? mappingContext 
				: new SemanticMappingContext(semanticDB.getNamespaces(), semanticDB.getDefaultNamespace());
	}	
	
	public MappingPolicy getMappingPolicy(Class<?> clazz){
		SemanticPersistentEntity<?> persistentEntity = getPersistentEntity(clazz);
		return persistentEntity.getMappingPolicy();
	}
	
	private SemanticPersistentEntity<?> getPersistentEntity(Class<?> targetClazz){
		return (SemanticPersistentEntityImpl<?>) mappingContext.getPersistentEntity(targetClazz);
	}	
		
	@Override
	public StatementsIterator getStatementsForResourceProperty(Object entity, SemanticPersistentProperty property){
		SemanticPersistentEntity<?> persistentEntity = getPersistentEntity(entity.getClass());
		URI uri = persistentEntity.getResourceId(entity);
		try {
			return new StatementsIterator(semanticDB.getStatementsForGraphQuery(
					EntityToGraphQueryConverter.getGraphQueryForResourceProperty(uri, persistentEntity, property)));
		} catch (Exception e) {
			throw ExceptionTranslator.translateExceptionIfPossible(e);
		}
	}	

	@Override
	public StatementsIterator getStatementsForResourceClass(URI resource, Class<?> clazz) {
		try {
			return new StatementsIterator(semanticDB.getStatementsForGraphQuery(
					EntityToGraphQueryConverter.getGraphQueryForResource(resource, getPersistentEntity(clazz))));
		} catch (Exception e) {
			throw ExceptionTranslator.translateExceptionIfPossible(e);
		} 
	}	
}