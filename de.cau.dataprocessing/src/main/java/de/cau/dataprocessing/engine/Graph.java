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
import com.google.common.collect.Maps;
import de.cau.dataprocessing.filters.IDataMangler;
import de.cau.dataprocessing.filters.annotations.InputPort;
import de.cau.dataprocessing.filters.annotations.OutputPort;
import de.cau.dataprocessing.reflect.InstanceMethod;

/**
 * This class describes a directed hyper graph of {@link IDataMangler} and their methods annotated
 * with {@link InputPort} and {@link OutputPort}. These methods are seen as ports and can be interconnected
 * (output* -> input*).
 *
 * @author Robert von Massow
 * @since 0.1
 *
 */
public class Graph {
	// connections in graph (from -> to)
	private Map<InstanceMethod<IDataMangler>, Collection<InstanceMethod<IDataMangler>>> mapping = new HashMap<InstanceMethod<IDataMangler>, Collection<InstanceMethod<IDataMangler>>>();

	private Map<IDataMangler, Iterable<InstanceMethod<IDataMangler>>> inPortsOf = new HashMap<IDataMangler, Iterable<InstanceMethod<IDataMangler>>>();

	private Map<IDataMangler, Iterable<InstanceMethod<IDataMangler>>> outPortsOf = new HashMap<IDataMangler, Iterable<InstanceMethod<IDataMangler>>>();

	private Map<IDataMangler, Iterable<InstanceMethod<IDataMangler>>> allNodes = new HashMap<IDataMangler, Iterable<InstanceMethod<IDataMangler>>>();

	private Map<IDataMangler, Map<IDataMangler, Integer>> idmMapping = new HashMap<IDataMangler, Map<IDataMangler, Integer>>();

	private List<IDataMangler> sources = new ArrayList<IDataMangler>();

	private List<IDataMangler> sinks = new ArrayList<IDataMangler>();

	/**
	 * Adds the given {@link IDataMangler} to the graph's nodes.<p>
	 *
	 * Its output and input ports are made available to the {@link #connect(InstanceMethod, InstanceMethod)} and can be retreived
	 * using the {@link #getAllPortsOf(IDataMangler)}, {@link #getOutPortsOf(IDataMangler)} and {@link #getInPortsOf(IDataMangler)}.
	 *
	 * If the given {@link IDataMangler} does not provide any input ports, it is considered to be a source,
	 * if it does not provide any output ports, it is a sink. These can also be retrieved using {@link #getAllSources()} or
	 * {@link #getAllSinks()} respectively.
	 * @param idm
	 */
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

	/**
	 * Connect an output port to input port.
	 *
	 * @param origin
	 * @param target
	 * @throws IllegalStateException if the {@link IDataMangler} defining either of the ports
	 * 		has not been added to the graph
	 * @throws IllegalArgumentException if the origin is not an output port or the target is not
	 * 		an input port
	 */
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
			connections = Lists.<InstanceMethod<IDataMangler>>newArrayList();
			this.mapping.put(origin, connections);
		}
		Map<IDataMangler, Integer> affected = idmMapping.get(origin.getInstance());
		if(affected == null) {
			affected = Maps.<IDataMangler, Integer>newHashMap();
			idmMapping.put(origin.getInstance(), affected);
		}
		if(affected.get(target.getInstance()) == null) {
			affected.put(target.getInstance(), 0);
		}
		if(!connections.contains(target)) {
			Iterables.<InstanceMethod<IDataMangler>>addAll(connections, Lists.<InstanceMethod<IDataMangler>>newArrayList(target));
			affected.put(target.getInstance(), affected.get(target.getInstance()) + 1);
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

	/**
	 * Disconnects two ports from each other
	 *
	 * @param sourcePort
	 * @param targetPort
	 * @return true on success
	 */
	public boolean disconnect(final InstanceMethod<IDataMangler> sourcePort,
			final InstanceMethod<IDataMangler> targetPort) {
		Collection<InstanceMethod<IDataMangler>> connections = mapping.get(sourcePort);
		if(connections != null) {
			Map<IDataMangler, Integer> map = idmMapping.get(sourcePort.getInstance());
			IDataMangler targetIDM = targetPort.getInstance();
			map.put(targetIDM, map.get(targetIDM) - 1);
			return Iterables.removeIf(connections, new Predicate<InstanceMethod<IDataMangler>>() {

				public boolean apply(InstanceMethod<IDataMangler> input) {
					return input == targetPort;
				}
			});
		}
		return false;
	}


	/**
	 * Removes the given {@link IDataMangler} from the graph, disconnecting all ports currently in use.
	 *
	 * @param idm
	 * @throws IllegalStateException if the node has not been added before
	 */
	public synchronized void removeIDM(IDataMangler idm) {
		if(allNodes.get(idm) == null) {
			throw new IllegalStateException();
		}
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
		for(IDataMangler e: idmMapping.keySet()) {
			idmMapping.get(e).remove(idm);
		}
		idmMapping.remove(idm);
		Iterables.removeAll(sources, Lists.newArrayList(idm));
		Iterables.removeAll(sinks, Lists.newArrayList(idm));
	}

	public Iterable<IDataMangler> getAllSources() {
		return Iterables.unmodifiableIterable(sources);
	}

	public Iterable<IDataMangler> getAllSinks() {
		return Iterables.unmodifiableIterable(sinks);
	}

	/**
	 * Returns all input ports of a previously added {@link IDataMangler}.
	 *
	 * @param idm
	 * @return the input ports
	 * @throws IllegalStateException if the node has not been added
	 */
	public Iterable<InstanceMethod<IDataMangler>> getInPortsOf(IDataMangler idm) {
		if(allNodes.get(idm) == null) {
			throw new IllegalStateException();
		}
		return Iterables.unmodifiableIterable(inPortsOf.get(idm));
	}

	/**
	 * Returns all output ports of a previously added {@link IDataMangler}.
	 *
	 * @param idm
	 * @return the output ports
	 * @throws IllegalStateException if the node has not been added
	 */
	public Iterable<InstanceMethod<IDataMangler>> getOutPortsOf(IDataMangler idm) {
		if(allNodes.get(idm) == null) {
			throw new IllegalStateException();
		}
		return Iterables.unmodifiableIterable(outPortsOf.get(idm));
	}

	public Iterable<InstanceMethod<IDataMangler>> getAllConnectionsToIDM(final IDataMangler idm) {
		Iterable<InstanceMethod<IDataMangler>> ports = Iterables.filter(mapping.keySet(), new Predicate<InstanceMethod<IDataMangler>>() {

			@Override
			public boolean apply(final InstanceMethod<IDataMangler> outPort) {
				Collection<InstanceMethod<IDataMangler>> inPorts = mapping.get(outPort);
				Iterable<InstanceMethod<IDataMangler>> filteredInPorts = Iterables.filter(inPorts, new Predicate<InstanceMethod<IDataMangler>>() {

					@Override
					public boolean apply(InstanceMethod<IDataMangler> inPort) {
						boolean b = inPort.getInstance() == idm;
						return b;
					}
				});
				return !Iterables.isEmpty(filteredInPorts);
			}

		});
		return Iterables.unmodifiableIterable(ports);
	}


	public Iterable<InstanceMethod<IDataMangler>> getAllConnectionsToInPort(final InstanceMethod<IDataMangler> inPort) {
		if(inPortsOf.get(inPort.getInstance()) == null) {
			throw new IllegalArgumentException();
		}
		Iterable<InstanceMethod<IDataMangler>> ports = Iterables.filter(mapping.keySet(), new Predicate<InstanceMethod<IDataMangler>>() {

			@Override
			public boolean apply(final InstanceMethod<IDataMangler> outPort) {
				Collection<InstanceMethod<IDataMangler>> connectionsToInPorts = mapping.get(outPort);
				Iterable<InstanceMethod<IDataMangler>> filtered = Iterables.filter(connectionsToInPorts, new Predicate<InstanceMethod<IDataMangler>>() {

					@Override
					public boolean apply(InstanceMethod<IDataMangler> inPortToBeChecked) {
						return inPortToBeChecked.equals(inPort);
					}
				});
				return Iterables.contains(filtered, inPort);
			}

		});
		return Iterables.unmodifiableIterable(ports);
	}

	public Iterable<InstanceMethod<IDataMangler>> getConnectionFromIDM(IDataMangler idm) {
		Iterable<InstanceMethod<IDataMangler>> outPorts = outPortsOf.get(idm);
		if(outPorts == null || Iterables.isEmpty(outPorts)) {
			return Collections.<InstanceMethod<IDataMangler>>emptyList();
		}
		return Iterables
				.unmodifiableIterable(Iterables.concat(Iterables.filter(
						Iterables
								.transform(
										outPorts,
										new Function<InstanceMethod<IDataMangler>, Iterable<InstanceMethod<IDataMangler>>>() {

											@Override
											public Iterable<InstanceMethod<IDataMangler>> apply(
													InstanceMethod<IDataMangler> input) {
												return mapping.get(input);
											}
										}),
						new Predicate<Iterable<InstanceMethod<IDataMangler>>>() {

							@Override
							public boolean apply(
									Iterable<InstanceMethod<IDataMangler>> input) {
								return input != null;
							}
						})));
	}

	public Iterable<IDataMangler> getFollowerIDMsOfIDM(
			IDataMangler idm) {
		Map<IDataMangler, Integer> map = idmMapping.get(idm);
		return map != null ? Iterables.unmodifiableIterable(map.keySet()) : Collections.<IDataMangler>emptyList();
	}


}
