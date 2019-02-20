package foo.bar.sample.model;

public class SomePojo extends ParentPojo {
    private String pojoField;

    public String getPojoField() {
        return pojoField;
    }

    public void setPojoField(final String pojoField) {
        this.pojoField = pojoField;
    }

    public static class InnerClass {
        public static class InnerInnerPojo {
            private final String someField;

            public InnerInnerPojo(final String someField) {
                this.someField = someField;
            }

            public String getSomeField() {
                return someField;
            }
        }
    }
}
