package com.reflection.proxy.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Created by Vasily Romanikhin on 26/02/15.
 */
public class Generator {
    private final String className;
    private final String packageName;
    private final ClassLoader classLoader;
    private String proxyClassName;

    public static final String PACKAGE_NONE    = "~package@none";

    public Generator(String pathToLib, String className, String packageName)
        throws IOException {
        this(new URLClassLoader(new URL[]{new File(pathToLib).toURI().toURL()}),
                className, packageName);
    }

    public Generator(String className, String packageName) {
        this(Generator.class.getClassLoader(), className, packageName);
    }

    private Generator(ClassLoader classLoader, String className, String packageName) {
        this.className   = className;
        this.packageName = packageName;
        this.classLoader = classLoader;
    }

    public GenerationResult generate() {
        StringBuilder sb = new StringBuilder();
        parseClass(sb, classLoader, className);
        return new GenerationResult(proxyClassName, sb.toString());
    }

    private void parseClass(StringBuilder sb,
                            ClassLoader classLoader,
                            String className) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            if (clazz.isInterface()) {
                parseInterface(sb, clazz);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseInterface(StringBuilder sb, Class<?> clazz) {
        proxyClassName = "Proxy" + clazz.getSimpleName();

        if (!packageName.equals(PACKAGE_NONE)) {
            appendln(sb, "package " + packageName);
        }
        appendln(sb, "import java.lang.reflect.Method;");
        appendln(sb);

        appendln(sb, String.format(
                     "public class %s implements %s {",
                          proxyClassName,
                          clazz.getCanonicalName()));
        appendln(sb, T1, "private final Object handle;");
        appendln(sb, T1, "private final ClassLoader loader;");
        appendln(sb, T1, "public " + proxyClassName + "(Object handle) {");
        appendln(sb, T2, "this.handle = handle;");
        appendln(sb, T2, "this.loader = handle.getClass().getClassLoader();");
        appendln(sb, T1, "}");

        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            parseMethod(sb, method);
        }
        appendln(sb, "}");
    }

    private void parseMethod(StringBuilder sb, Method method) {
        appendln(sb, "    @Override");

        resetValueName();
        Class<?> retType = method.getReturnType();
        Class<?> params[] = method.getParameterTypes();
        appendln(sb, String.format(
                "    public %s %s(%s) {",
                retType.getName(),
                method.getName(),
                String.join(", ", Arrays.asList(params).stream().
                        map(p -> (p.getCanonicalName() + " " + getValueName())).
                        collect(toList()))));
        methodBody(sb, method);
        appendln(sb, "    }");
    }

    private void methodBody(StringBuilder sb, Method m) {
        Class<?> retType = m.getReturnType();
        Class<?> params[] = m.getParameterTypes();

        // Getting method via reflection
        String argTypes = String.join(", ", Arrays.asList(params).stream().
                map(p -> p.isPrimitive() ? (Class<?>) PRIMITIVE_CLASSES.get(p) : p).
                map(p -> p.getClassLoader() == String.class.getClassLoader() ?
                    String.format("%s.class", p.getCanonicalName()) :
                    String.format("loader.loadClass(\"%s\")", p.getCanonicalName())).
                collect(toList()));
        appendln(sb, T2, "try {");

        appendln(sb, T3, "Method m = handle.getClass().getMethod" +
                (params.length == 0 ?
                String.format("(\"%s\");", m.getName()) :
                String.format("(\"%s\", %s);", m.getName(), argTypes)));

        // Method invocation
        resetValueName();
        String args = String.join(", ", Arrays.asList(params).stream().
                map(p -> p.getClassLoader() == String.class.getClassLoader() ?
                        getValueName() :
                        String.format("%s.convertTo(%s)",
                                p.getCanonicalName(), getValueName())).
                collect(toList()));
        appendln(sb, T3, "Object result = " +
                (argTypes.length() == 0 ?
                "m.invoke(handle);":
                String.format("m.invoke(handle, %s);", args)));
        resetValueName();

        // Return result
        if (!retType.equals(Void.TYPE)) {
            appendln(sb, T3,
                retType.getClassLoader() == String.class.getClassLoader() ?
                String.format("return (%s) result;",
                        wrap(retType).getCanonicalName()) :
                String.format("return %s.convertFrom(result);",
                        retType.getCanonicalName())
            );
        }

        appendln(sb, T2, "} catch (Exception e) {");
        appendln(sb, T3, "e.printStackTrace();");
        appendln(sb, T3, "throw new RuntimeException(e);");
        appendln(sb, T2, "} ");
    }

    private static Class<?> wrap(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVE_CLASSES.get(clazz) : clazz;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_CLASSES = 
        new HashMap<>();
    static {
        PRIMITIVE_CLASSES.put(boolean.class, Boolean.class);
        PRIMITIVE_CLASSES.put(byte.class, Byte.class);
        PRIMITIVE_CLASSES.put(char.class, Character.class);
        PRIMITIVE_CLASSES.put(double.class, Double.class);
        PRIMITIVE_CLASSES.put(float.class, Float.class);
        PRIMITIVE_CLASSES.put(int.class, Integer.class);
        PRIMITIVE_CLASSES.put(long.class, Long.class);
        PRIMITIVE_CLASSES.put(short.class, Short.class);
        PRIMITIVE_CLASSES.put(void.class, Void.class);
    }

    private static int valCount = 0;
    private static void resetValueName() {
        valCount = 0;
    }
    private static String getValueName() {
        return "val" + valCount++;
    }
    private final String T1 = "    ";
    private final String T2 = "        ";
    private final String T3 = "            ";

    private static void appendln(StringBuilder sb) {
        sb.append(System.getProperty("line.separator"));
    }

    private static void appendln(StringBuilder sb, String... strs) {
        Arrays.asList(strs).stream().forEach(s -> sb.append(s));
        sb.append(System.getProperty("line.separator"));
    }

    public static class GenerationResult {
        private final String className;
        private final String classBody;

        public GenerationResult(String className, String classBody) {
            this.className = className;
            this.classBody = classBody;
        }

        public String getClassName() {
            return className;
        }

        public String getClassBody() {
            return classBody;
        }
    }

}
