package some.pck.model;

import some.pck.model.childpackage.ChildModel;


public class ParentModel {
	private final String somePropertyOfTypeString;
	private final Integer somePropertyOfTypeInteger;
	private final ChildModel someChildModel;

	public ParentModel(final String somePropertyOfTypeString, final Integer somePropertyOfTypeInteger,
		final ChildModel someChildModel) {
		super();
		this.somePropertyOfTypeString = somePropertyOfTypeString;
		this.somePropertyOfTypeInteger = somePropertyOfTypeInteger;
		this.someChildModel = someChildModel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((someChildModel == null) ? 0 : someChildModel.hashCode());
		result = (prime * result) + ((somePropertyOfTypeInteger == null) ? 0 : somePropertyOfTypeInteger.hashCode());
		result = (prime * result) + ((somePropertyOfTypeString == null) ? 0 : somePropertyOfTypeString.hashCode());
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
		if (!(obj instanceof ParentModel)) {
			return false;
		}
		final ParentModel other = (ParentModel) obj;
		if (someChildModel == null) {
			if (other.someChildModel != null) {
				return false;
			}
		} else if (!someChildModel.equals(other.someChildModel)) {
			return false;
		}
		if (somePropertyOfTypeInteger == null) {
			if (other.somePropertyOfTypeInteger != null) {
				return false;
			}
		} else if (!somePropertyOfTypeInteger.equals(other.somePropertyOfTypeInteger)) {
			return false;
		}
		if (somePropertyOfTypeString == null) {
			if (other.somePropertyOfTypeString != null) {
				return false;
			}
		} else if (!somePropertyOfTypeString.equals(other.somePropertyOfTypeString)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ParentModel [somePropertyOfTypeString=" + somePropertyOfTypeString + ", somePropertyOfTypeInteger=" +
			somePropertyOfTypeInteger + ", someChildModel=" + someChildModel + "]";
	}

}
