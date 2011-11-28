package de.cau.dataprocessing.filters;

public interface IDataMangler {

	String getName();

	void execute();

	static abstract class AbstractDataMangler implements IDataMangler {

		private String name;

		public AbstractDataMangler() {
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
