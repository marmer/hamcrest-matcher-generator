package sample.classes;

public class ComplexSample {

	public void someMethod() { }

	public static class InnerStaticClass {
		Runnable inlineClass = new Runnable() {
			@Override
			public void run() { }
		};
	}

	public static class InnerClass { }
}
