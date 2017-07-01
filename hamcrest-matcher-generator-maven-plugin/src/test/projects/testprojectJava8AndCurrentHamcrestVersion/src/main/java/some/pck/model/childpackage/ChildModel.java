package some.pck.model.childpackage;

public class ChildModel {
	private final String childModelProperty;

	public ChildModel(final String childModelProperty) {
		this.childModelProperty = childModelProperty;
	}

	@Override
	public String toString() {
		return "ChildModel [childModelProperty=" + childModelProperty + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((childModelProperty == null) ? 0 : childModelProperty.hashCode());
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
		if (!(obj instanceof ChildModel)) {
			return false;
		}
		final ChildModel other = (ChildModel) obj;
		if (childModelProperty == null) {
			if (other.childModelProperty != null) {
				return false;
			}
		} else if (!childModelProperty.equals(other.childModelProperty)) {
			return false;
		}
		return true;
	}

	public String getChildModelProperty() {
		return childModelProperty;
	}
}
