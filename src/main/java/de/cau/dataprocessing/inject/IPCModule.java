package de.cau.dataprocessing.inject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import de.cau.dataprocessing.engine.Result;
import de.cau.dataprocessing.filters.IDataMangler;
import de.cau.dataprocessing.reflect.InstanceMethod;

public class IPCModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(new TypeLiteral<Map<InstanceMethod<IDataMangler>, Iterable<Method>>>(){})
			.to(new TypeLiteral<ConcurrentHashMap<InstanceMethod<IDataMangler>, Iterable<Method>>>(){});
		bind(new TypeLiteral<Map<Method, IDataMangler>>(){})
			.to(new TypeLiteral<ConcurrentHashMap<Method, IDataMangler>>(){});
		bind(Map.class).to(HashMap.class);
		bind(new TypeLiteral<BlockingQueue<Result>>(){})
			.to(new TypeLiteral<LinkedBlockingQueue<Result>>(){});
	}

}
