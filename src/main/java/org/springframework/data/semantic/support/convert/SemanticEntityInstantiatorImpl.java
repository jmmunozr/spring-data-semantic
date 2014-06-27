package org.springframework.data.semantic.support.convert;

import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.ReflectionEntityInstantiator;
import org.springframework.data.semantic.convert.SemanticEntityInstantiator;
import org.springframework.data.semantic.core.StatementsIterator;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;

public class SemanticEntityInstantiatorImpl implements SemanticEntityInstantiator{

	private EntityInstantiator instantiator = ReflectionEntityInstantiator.INSTANCE;

	@Override
	public <T> T createInstanceFromState(SemanticPersistentEntity<T> entity,
			StatementsIterator statements) {
		T instance = instantiator.createInstance(entity, null);
		entity.setPersistentState(instance, statements);
		return instance;
	}

}