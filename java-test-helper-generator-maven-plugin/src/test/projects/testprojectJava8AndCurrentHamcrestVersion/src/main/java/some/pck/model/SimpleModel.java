package some.pck.model;

public class SimpleModel {
	private final String someProperty;

	public SimpleModel(final String someProperty) {
		this.someProperty = someProperty;
	}

	@Override
	public String toString() {
		return "SimpleModel [someProperty=" + someProperty + "]";
	}

	public String getSomeProperty() {
		return someProperty;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((someProperty == null) ? 0 : someProperty.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SimpleModel)) {
			return false;
		}
		final SimpleModel other = (SimpleModel) obj;
		if (someProperty == null) {
			if (other.someProperty != null) {
				return false;
			}
		} else if (!someProperty.equals(other.someProperty)) {
			return false;
		}
		return true;
	}
}
