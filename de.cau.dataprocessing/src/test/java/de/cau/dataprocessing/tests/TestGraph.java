package de.cau.dataprocessing.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Injector;

import de.cau.dataprocessing.engine.Graph;
import de.cau.dataprocessing.filters.IDataMangler;
import de.cau.dataprocessing.reflect.InstanceMethod;
import de.cau.dataprocessing.tests.data.SimpleTestSink;
import de.cau.dataprocessing.tests.data.SimpleTestSource;

public class TestGraph {

	private Injector inj;
	private Graph g;

	@Before
	public void setUp() throws Exception {
		inj = Guice.createInjector();
		g = inj.getInstance(Graph.class);
	}

	@AfterClass
	public static void afterClass() {
	}

	@BeforeClass
	public static void beforeClass() {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreation() {
		assertNotNull("Creation of Graph failed", g);
	}

	@Test
	public void testAddDataMangler() throws Exception {
		IDataMangler idm = new IDataMangler() {

			@Override
			public String getName() {
				return "Empty test mangler";
			}

			@Override
			public void execute() {
			}
		};
		g.addMangler(idm);
		assertTrue("Test mangler not in graph", Iterables.contains(g.getAllIDMs(), idm));
	}

	@Test
	public void testConnectionsIDMs() throws Exception {
		IDataMangler source = inj.getInstance(SimpleTestSource.class);
		IDataMangler sink = inj.getInstance(SimpleTestSink.class);
		IDataMangler sink2 = inj.getInstance(SimpleTestSink.class);
		g.addMangler(source);
		g.addMangler(sink);
		Iterable<InstanceMethod<IDataMangler>> sourcePorts = g.getAllPortsOf(source);
		assertEquals(1, Iterables.size(sourcePorts));
		Iterable<InstanceMethod<IDataMangler>> sinkPorts = g.getAllPortsOf(sink);
		assertEquals(1, Iterables.size(sinkPorts));
		InstanceMethod<IDataMangler> sourcePort = sourcePorts.iterator().next();
		InstanceMethod<IDataMangler> sinkPort = sinkPorts.iterator().next();
		g.connect(sourcePort, sinkPort);
		Iterable<InstanceMethod<IDataMangler>> targets = g.getConnectionsFromOutputPort(sourcePort);
		assertTrue("IDMs not connected", Iterables.size(targets) == 1 && Iterables.contains(targets, sinkPort));
		assertTrue("graph.isConnected(source, target) inconsistent with graph's state", g.isConnected(sourcePort, sinkPort));
		g.disconnect(sourcePort, sinkPort);
		assertFalse("Ports still connected after disconnect", g.isConnected(sourcePort, sinkPort));
		// reconnect ports
		g.connect(sourcePort, sinkPort);
		g.addMangler(sink2);
		Iterable<InstanceMethod<IDataMangler>> sink2Ports = g.getAllPortsOf(sink2);
		InstanceMethod<IDataMangler> sink2Port = sink2Ports.iterator().next();
		g.connect(sourcePort, sink2Port);
		assertTrue(Iterables.size(g.getAllConnectionsToIDM(sink2)) == 1 && g.getAllConnectionsToIDM(sink2).iterator().next() == sourcePort);
		assertTrue(Iterables.size(g.getAllConnectionsToInPort(sink2Port)) == 1 && g.getAllConnectionsToInPort(sink2Port).iterator().next() == sourcePort);
		assertTrue("Source not connected to sink2", g.isConnected(sourcePort, sink2Port));
		Iterable<InstanceMethod<IDataMangler>> targetPorts = g.getConnectionFromIDM(source);
		assertTrue(Iterables.size(targetPorts) == 2 && Iterables.contains(targetPorts, sinkPort) && Iterables.contains(targetPorts, sink2Port));
		Iterable<IDataMangler> targetIDMs = g.getFollowerIDMsOfIDM(source);
		assertTrue(Iterables.size(targetIDMs) == 2 && Iterables.contains(targetIDMs, sink) && Iterables.contains(targetIDMs, sink2));
		g.removeIDM(source);
		assertFalse("Test source was not removed from all IDMs", Iterables.contains(g.getAllIDMs(), source));
		assertFalse("Test source was not removed from sources", Iterables.contains(g.getAllSources(), source));;
		assertFalse("Connection still existent after remove", g.isConnected(sourcePort, sinkPort));
		// test illegal connection throws exception
		g.addMangler(source);
		g.connect(sourcePort, sinkPort);
		try {
			g.connect(sinkPort, sourcePort);
			fail("Connection from InputPort to OutputPort is prohibited");
		} catch (IllegalArgumentException e) {
		}
	}
}
