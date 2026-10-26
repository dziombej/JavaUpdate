package com.tomtom.genesis.platform.jdk24;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Java 24 (JEP 484) - the Class-File API.
 *
 * A standard, JDK-maintained library for reading, writing and transforming
 * .class files - replacing the bundled fork of ASM that the JDK used internally
 * and that everyone else had to depend on separately.
 */
public class ClassFileApiDemo {

    public static void main(String[] args) throws Exception {
        whyItExists();
        reading();
        generating();
        notes();
    }

    private static void whyItExists() {
        header("Why the JDK grew its own");

        System.out.println("Bytecode manipulation has always meant a third-party library: ASM,");
        System.out.println("Byte Buddy, Javassist, cglib. The JDK itself bundled a private copy of");
        System.out.println("ASM for Lambda metafactory, jlink and friends.");
        System.out.println();
        System.out.println("That created a chronic problem: every new class-file version needs a new");
        System.out.println("ASM, and with a six-month release cadence, libraries that generate");
        System.out.println("bytecode break twice a year. Anyone who upgraded a JDK and hit");
        System.out.println("'Unsupported class file major version 65' has met it.");
        System.out.println();
        System.out.println("A class-file library that ships WITH the JDK is, by construction, never");
        System.out.println("out of date with the format.");
    }

    private static void reading() throws Exception {
        header("Reading a class file");

        // Read this very class back from disk.
        Path classFile = locateOwnClassFile();
        if (classFile == null) {
            System.out.println("(compiled class not found on disk - run via target/classes to see this)");
            return;
        }

        ClassModel model = ClassFile.of().parse(Files.readAllBytes(classFile));

        System.out.println("class name    = " + model.thisClass().asInternalName());
        System.out.println("superclass    = " + model.superclass()
                .map(c -> c.asInternalName()).orElse("(none)"));
        System.out.println("major version = " + model.majorVersion()
                + "   (" + (model.majorVersion() - 44) + " = the Java release)");
        System.out.println("source file   = " + model.findAttribute(java.lang.classfile.Attributes.sourceFile())
                .map(SourceFileAttribute::sourceFile).map(Object::toString).orElse("(none)"));
        System.out.println("methods       = " + model.methods().size()
                + ", fields = " + model.fields().size());

        System.out.println();
        System.out.println("method signatures:");
        String methods = model.methods().stream()
                .map(m -> "  " + m.methodName().stringValue() + m.methodType().stringValue())
                .collect(Collectors.joining("\n"));
        System.out.println(methods);

        // Counting instructions in one method body.
        model.methods().stream()
                .filter(m -> m.methodName().equalsString("header"))
                .findFirst()
                .flatMap(MethodModel::code)
                .ifPresent(code -> System.out.println("\n'header' method body has "
                        + code.elementList().size() + " code elements"));

        System.out.println();
        System.out.println("Everything is immutable and tree-shaped: ClassModel contains MethodModel");
        System.out.println("contains CodeModel contains instructions. No visitor callbacks, no");
        System.out.println("mutable builder state to keep straight - the main departure from ASM.");
    }

    private static void generating() throws Exception {
        header("Generating a class at runtime");

        ClassDesc helloClass = ClassDesc.of("GeneratedHello");
        ClassDesc stringClass = ClassDesc.of("java.lang.String");

        byte[] bytes = ClassFile.of().build(helloClass, builder -> builder
                .withFlags(ClassFile.ACC_PUBLIC)
                .withMethodBody(
                        "greet",
                        MethodTypeDesc.of(stringClass, stringClass),        // String greet(String)
                        ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                        code -> code
                                .ldc("Hello, ")
                                .aload(0)
                                .invokevirtual(stringClass, "concat",
                                        MethodTypeDesc.of(stringClass, stringClass))
                                .areturn()));

        System.out.println("generated " + bytes.length + " bytes of class file");

        // Load it and call it.
        Class<?> generated = new ClassLoader() {
            Class<?> define(byte[] b) {
                return defineClass("GeneratedHello", b, 0, b.length);
            }
        }.define(bytes);

        Method greet = generated.getMethod("greet", String.class);
        System.out.println("GeneratedHello.greet(\"world\") = " + greet.invoke(null, "world"));

        System.out.println();
        System.out.println("The code builder mirrors the JVM instruction set: ldc, aload, invokevirtual,");
        System.out.println("areturn. It also offers higher-level helpers that pick the right opcode");
        System.out.println("for you, so you are not forced down to raw bytecode for common shapes.");
    }

    private static void notes() {
        header("Practical notes");

        System.out.println("Who this is for: framework and tool authors - agents, mocking libraries,");
        System.out.println("instrumentation, static analysis, build plugins. Ordinary application");
        System.out.println("code will never touch it.");
        System.out.println();
        System.out.println("Why it is worth KNOWING about even if you never call it:");
        System.out.println("  - it explains why 'Unsupported class file major version' errors should");
        System.out.println("    become rarer as libraries migrate off ASM;");
        System.out.println("  - it is in java.base, so there is no dependency and no shading;");
        System.out.println("  - the major version arithmetic (major - 44 = Java release) is a handy");
        System.out.println("    thing to remember when reading such an error: 65 means Java 21.");
        System.out.println();
        System.out.println("The API is also the reference for the class-file format itself - the");
        System.out.println("javadoc of java.lang.classfile.Attributes is a readable index of every");
        System.out.println("attribute the format defines.");
    }

    private static Path locateOwnClassFile() {
        Path candidate = Path.of("target", "classes",
                "com", "tomtom", "genesis", "platform", "jdk24", "ClassFileApiDemo.class");
        return Files.exists(candidate) ? candidate : null;
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
