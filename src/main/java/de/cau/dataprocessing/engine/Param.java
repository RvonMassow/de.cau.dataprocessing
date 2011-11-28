package de.cau.dataprocessing.engine;

import java.lang.reflect.Method;
import java.util.Map;

public class Param {

	private Map<Method, Object> map;

	public void putForPort(Method p, Object data) {
		map.put(p, data);
	}

	public Object getForPort(Method p) {
		return map.get(p);
	}
}
