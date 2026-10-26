package foo.bar.sample.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class SomeLombokPojo {
    private String someProp;
    @Getter(AccessLevel.PACKAGE)
    private String someNoProp;
}
