package de.cau.dataprocessing.engine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.cau.dataprocessing.filters.IDataMangler;
import de.cau.dataprocessing.filters.annotations.InputPort;
import de.cau.dataprocessing.filters.annotations.OutputPort;
import de.cau.dataprocessing.reflect.InstanceMethod;

public class Graph {
	// connections in graph (from -> to)
	private Map<InstanceMethod<IDataMangler>, Collection<InstanceMethod<IDataMangler>>> mapping = new HashMap<InstanceMethod<IDataMangler>, Collection<InstanceMethod<IDataMangler>>>();

	private Map<IDataMangler, Iterable<InstanceMethod<IDataMangler>>> inPortsOf = new HashMap<IDataMangler, Iterable<InstanceMethod<IDataMangler>>>();

	private Map<IDataMangler, Iterable<InstanceMethod<IDataMangler>>> outPortsOf = new HashMap<IDataMangler, Iterable<InstanceMethod<IDataMangler>>>();

	private Map<IDataMangler, Iterable<InstanceMethod<IDataMangler>>> allNodes = new HashMap<IDataMangler, Iterable<InstanceMethod<IDataMangler>>>();

	private List<IDataMangler> sources = new ArrayList<IDataMangler>();

	private List<IDataMangler> sinks = new ArrayList<IDataMangler>();

	// TODO remove
	public void addMangler(final IDataMangler idm) {
		Class<? extends IDataMangler> clazz = idm.getClass();
		List<Method> methods = Arrays.asList(clazz.getMethods());
		// collect inputs
		Collection<Method> iPorts = Collections2.<Method>filter(methods, new Predicate<Method>() {

			public boolean apply(Method input) {
				return input.getAnnotation(InputPort.class) != null;
			}

		});
		// if none: source, else cache them
		if(iPorts.isEmpty()) {
			sources.add(idm);
		}
		inPortsOf.put(idm, Collections2.transform(iPorts, new Function<Method, InstanceMethod<IDataMangler>>() {

			public InstanceMethod<IDataMangler> apply(Method input) {
				return new InstanceMethod<IDataMangler>(idm, input);
			}
		}));
		Collection<Method> oPorts = Collections2.<Method>filter(methods, new Predicate<Method>() {

			public boolean apply(Method input) {
				return input.getAnnotation(OutputPort.class) != null;
			}

		});
		// if none: sink, else cache them
		if(oPorts.isEmpty()) {
			sinks.add(idm);
		}
		outPortsOf.put(idm, Collections2.transform(oPorts, new Function<Method, InstanceMethod<IDataMangler>>() {

			public InstanceMethod<IDataMangler> apply(Method input) {
				return new InstanceMethod<IDataMangler>(idm, input);
			}
		}));
		allNodes.put(idm, Iterables.concat(inPortsOf.get(idm), outPortsOf.get(idm)));
	}

	@SuppressWarnings("unchecked")
	public void connect(InstanceMethod<IDataMangler> origin, InstanceMethod<IDataMangler> target) {
		if(allNodes.get(origin.getInstance()) == null) {
			throw new IllegalStateException(origin + " not added to graph");
		}
		if(allNodes.get(target.getInstance()) == null) {
			throw new IllegalStateException(target + " not added to graph");
		}
		if(Iterables.isEmpty(outPortsOf.get(origin.getInstance()))) {
			throw new IllegalArgumentException("source " + origin + " is not well defined");
		}
		if(Iterables.isEmpty(inPortsOf.get(target.getInstance()))) {
			throw new IllegalArgumentException("target " +target + " is not well defined");
		}
		Collection<InstanceMethod<IDataMangler>> connections = this.mapping.get(origin);
		if(connections == null) {
			connections = new ArrayList<InstanceMethod<IDataMangler>>();
			this.mapping.put(origin, connections);
		}
		if(!connections.contains(target)) {
			Iterables.<InstanceMethod<IDataMangler>>addAll(connections, Lists.<InstanceMethod<IDataMangler>>newArrayList(target));
		}
	}

	public Iterable<IDataMangler> getAllIDMs() {
		return Iterables.unmodifiableIterable(allNodes.keySet());
	}

	public Iterable<InstanceMethod<IDataMangler>> getAllPortsOf(IDataMangler idm) {
		if(allNodes.get(idm) == null) {
			throw new IllegalStateException(idm + " not added to graph");
		}
		Iterable<InstanceMethod<IDataMangler>> nodes = allNodes.get(idm);
		return nodes != null ? Iterables.unmodifiableIterable(nodes)
				: Collections.<InstanceMethod<IDataMangler>>emptyList();
	}

	public Iterable<InstanceMethod<IDataMangler>> getConnectionsFromOutputPort(InstanceMethod<IDataMangler> port) {
		Collection<InstanceMethod<IDataMangler>> targets = mapping.get(port);
		return targets != null ? Iterables.unmodifiableIterable(targets)
			: Collections.<InstanceMethod<IDataMangler>>emptyList();
	}

	public boolean isConnected(InstanceMethod<IDataMangler> source, InstanceMethod<IDataMangler> target) {
		Collection<InstanceMethod<IDataMangler>> collection = mapping.get(source);
		if(collection != null) {
			return Iterables.contains(collection, target);
		} else {
			return false;
		}
	}

	public boolean disconnect(final InstanceMethod<IDataMangler> sourcePort,
			final InstanceMethod<IDataMangler> targetPort) {
		Collection<InstanceMethod<IDataMangler>> connections = mapping.get(sourcePort);
		if(connections != null) {
			return Iterables.removeIf(connections, new Predicate<InstanceMethod<IDataMangler>>() {

				public boolean apply(InstanceMethod<IDataMangler> input) {
					return input == targetPort;
				}
			});
		}
		return false;
	}

	public synchronized void removeIDM(IDataMangler idm) {
		final Iterable<InstanceMethod<IDataMangler>> outPorts = outPortsOf.get(idm);
		final Iterable<InstanceMethod<IDataMangler>> inPorts = inPortsOf.get(idm);
		for(InstanceMethod<IDataMangler> p : outPorts) {
			mapping.remove(p);
		}
		for(InstanceMethod<IDataMangler> connections : mapping.keySet()) {
			Collection<InstanceMethod<IDataMangler>> fromTo = mapping.get(connections);
			Collection<InstanceMethod<IDataMangler>> filtered = Collections2.filter(fromTo, new Predicate<InstanceMethod<IDataMangler>>() {

				public boolean apply(InstanceMethod<IDataMangler> input) {
					return Iterables.contains(inPorts, input);
				}
			});
			mapping.put(connections, filtered);
		}
		allNodes.remove(idm);
		Iterables.removeAll(sources, Lists.newArrayList(idm));
		Iterables.removeAll(sinks, Lists.newArrayList(idm));
	}

	public Iterable<IDataMangler> getAllSources() {
		return Iterables.unmodifiableIterable(sources);
	}

	public Iterable<IDataMangler> getAllSinks() {
		return Iterables.unmodifiableIterable(sinks);
	}

	public Iterable<InstanceMethod<IDataMangler>> getInPortsOf(IDataMangler idm) {
		return Iterables.unmodifiableIterable(inPortsOf.get(idm));
	}

	public Iterable<InstanceMethod<IDataMangler>> getOutPortsOf(IDataMangler idm) {
		return Iterables.unmodifiableIterable(outPortsOf.get(idm));
	}
}
