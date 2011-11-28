package de.cau.dataprocessing.reflect;

import java.lang.reflect.Method;

public class InstanceMethod<T> {

	private Method method;
	private T instance;

	public InstanceMethod(T instance, Method m) {
		this.instance = instance;
		this.method = m;
	}

	@Override
	public int hashCode() {
		return instance.hashCode() ^ method.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof InstanceMethod<?>
			&& instance == ((InstanceMethod<?>)obj).instance
			&& method == ((InstanceMethod<?>)obj).method;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method m) {
		this.method = m;
	}

	public T getInstance() {
		return instance;
	}

	public void setInstance(T instance) {
		this.instance = instance;
	}
}
