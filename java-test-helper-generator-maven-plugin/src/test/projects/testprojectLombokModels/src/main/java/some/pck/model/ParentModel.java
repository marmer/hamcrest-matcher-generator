package some.pck.model;

import lombok.Value;

import some.pck.model.childpackage.ChildModel;


@Value
public class ParentModel {
	private final String somePropertyOfTypeString;
	private final Integer somePropertyOfTypeInteger;
	private final ChildModel someChildModel;
}
