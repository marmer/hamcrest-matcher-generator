package sample2.classes;

import lombok.Getter;

@Getter
public class ContainerClass {
    private String containerProp;

    @Getter
    public class InnerClass {
        private String innerProp;

        @Getter
        public class InnerInnerClass {
            private String innerInnerProp;
        }
    }
}
