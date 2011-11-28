package de.cau.dataprocessing;

import com.google.inject.Guice;

import de.cau.dataprocessing.engine.Graph;
import de.cau.dataprocessing.inject.IPCModule;

public class Main {
	public static void main(String[] args) {
		Guice.createInjector(new IPCModule()).getInstance(Graph.class);
	}
}
