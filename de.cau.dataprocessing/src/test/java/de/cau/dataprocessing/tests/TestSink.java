package de.cau.dataprocessing.tests;

import de.cau.dataprocessing.filters.IDataMangler;
import de.cau.dataprocessing.filters.annotations.InputPort;

public class TestSink implements IDataMangler {

	@Override
	public String getName() {
		return "Empty test Sink";
	}

	@InputPort
	public void setFoo(Integer i) {

	}

	@Override
	public void execute() {

	}


}
