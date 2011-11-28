package de.cau.dataprocessing.engine;

import java.lang.reflect.Method;
import java.util.Map;

import de.cau.dataprocessing.filters.IDataMangler;

public class Result {

	private IDataMangler src;

	private Map<Method, Object> data;

	public IDataMangler getSrc() {
		return src;
	}

	public void setSrc(IDataMangler src) {
		this.src = src;
	}

	public Object getData(Method o) {
		return data.get(o);
	}

	public void putForPort(Method o, Object data) {
		this.data.put(o, data);
	}

	public Iterable<Method> methods() {
		return data.keySet();
	}

}
