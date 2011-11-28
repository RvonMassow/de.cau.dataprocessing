package de.cau.dataprocessing.tests;

import de.cau.dataprocessing.filters.IDataMangler;
import de.cau.dataprocessing.filters.annotations.OutputPort;

public class TestSource implements IDataMangler {

	@Override
	public String getName() {
		return "Empty test Source";
	}

	@OutputPort
	public Integer getFoo() {
		return 42;
	}

	@Override
	public void execute() {
	}
}
