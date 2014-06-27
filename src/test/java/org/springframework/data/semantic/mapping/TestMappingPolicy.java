package org.springframework.data.semantic.mapping;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestMappingPolicy {
	
	@Test
	public void testDefaultMP(){
		assertFalse(MappingPolicy.DEFAULT_POLICY.useDirty());
		assertTrue(MappingPolicy.DEFAULT_POLICY.eagerLoad());
	}
	
	@Test
	public void testDirectAccessMP(){
		assertTrue(MappingPolicy.MAP_FIELD_DIRECT_POLICY.useDirty());
		assertTrue(MappingPolicy.MAP_FIELD_DIRECT_POLICY.eagerLoad());
	}
	
	@Test
	public void testShouldLoadMP(){
		assertFalse(MappingPolicy.LAZY_LOAD_POLICY.eagerLoad());
		assertFalse(MappingPolicy.LAZY_LOAD_POLICY.useDirty());
	}
	
	@Test
	public void testCombinationMP(){
		MappingPolicy mp = MappingPolicy.MAP_FIELD_DIRECT_POLICY.combineWith(MappingPolicy.LAZY_LOAD_POLICY);
		assertTrue(mp.useDirty());
		assertFalse(mp.eagerLoad());
	}

}
