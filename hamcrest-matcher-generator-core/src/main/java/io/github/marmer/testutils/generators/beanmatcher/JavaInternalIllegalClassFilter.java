package io.github.marmer.testutils.generators.beanmatcher;

import java.util.List;
import java.util.stream.Collectors;


public class JavaInternalIllegalClassFilter implements IllegalClassFilter {
    @Override
    public List<Class<?>> filter(final List<Class<?>> baseClassList) {
        return baseClassList.stream().filter(this::isNoJavaLangClass).collect(Collectors.toList());
    }

    private boolean isNoJavaLangClass(final Class<?> clazz) {
        return !clazz.getPackage().getName().startsWith("java.lang");
    }
}
