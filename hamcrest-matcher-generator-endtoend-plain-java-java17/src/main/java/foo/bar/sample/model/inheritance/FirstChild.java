package foo.bar.sample.model.inheritance;

import java.util.UUID;

public class FirstChild extends Parent {
	private final String name;

	public FirstChild(final UUID id, final String name) {
		super(id);
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
