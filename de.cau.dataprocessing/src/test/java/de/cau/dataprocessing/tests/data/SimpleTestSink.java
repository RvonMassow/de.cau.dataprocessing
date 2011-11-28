package de.cau.dataprocessing.tests.data;

import de.cau.dataprocessing.filters.IDataMangler;
import de.cau.dataprocessing.filters.annotations.InputPort;


public class SimpleTestSink implements IDataMangler {

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
