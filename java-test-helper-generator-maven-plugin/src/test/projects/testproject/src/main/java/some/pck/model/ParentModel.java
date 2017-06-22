package some.pck.model;

import lombok.Value;

import some.pck.model.childpackage.ChildModel;


@Value
public class ParentModel {
	private String somePropertyOfTypeString;
	private Integer somePropertyOfTypeInteger;
	private ChildModel someChildModel;
}
