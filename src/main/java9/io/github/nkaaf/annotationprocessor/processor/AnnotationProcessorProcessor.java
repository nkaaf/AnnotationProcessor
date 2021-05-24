package io.github.nkaaf.annotationprocessor.processor;

/*-
 * #%L
 * AnnotationProcessor
 * %%
 * Copyright (C) 2021 Niklas Kaaf
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import io.github.nkaaf.annotationprocessor.annotation.AnnotationProcessor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Processor for {@link AnnotationProcessor}.
 * </p>
 *
 * <p>
 * This processor checks if your annotation processor is set up compliant to JSP 269. If your processor is set up
 * correctly, its canonical name will be added to the service file. If the service file does not exist, it will be
 * created.
 * </p>
 *
 * @author Niklas Kaaf
 * 
 * @version 1.0
 * 
 * @see <a href="https://www.jcp.org/en/jsr/detail?id=269">JSP 269</a>
 * 
 * @since 1.0
 */
@SupportedAnnotationTypes("io.github.nkaaf.annotationprocessor.annotation.AnnotationProcessor")
public class AnnotationProcessorProcessor extends AbstractProcessor {

    private static final String PROCESSOR_SERVICE_FILE = "META-INF" + File.separator + "services" + File.separator
            + Processor.class.getCanonicalName();

    boolean generateServiceFile = true;

    private TypeElement annotationProcessorType;
    private Set<TypeElement> annotatedClasses;

    /**
     * Check if method exist.
     *
     * @param methods
     *            {@link List} of methods
     * @param expectedMethodName
     *            Name of the expected method
     * @param expectedParamSize
     *            Size of parameters
     * @param expectedParamTypes
     *            {@link List} of parameter types
     * @param expectedReturnType
     *            Type of return value
     *
     * @return true if method exist, false otherwise
     */
    private static boolean checkIfMethodExists(List<ExecutableElement> methods, String expectedMethodName,
            int expectedParamSize, List<String> expectedParamTypes, String expectedReturnType) {
        for (ExecutableElement method : methods) {
            if (method.getSimpleName().contentEquals(expectedMethodName)) {
                if (method.getParameters().size() == expectedParamSize) {
                    List<? extends VariableElement> params = method.getParameters();
                    boolean matchAllParams = true;
                    for (int i = 0; i < params.size(); i++) {
                        if (!matchAllParams) {
                            break;
                        }
                        String paramType = params.get(i).asType().toString();
                        matchAllParams = paramType.equals(expectedParamTypes.get(i));
                    }
                    if (matchAllParams) {
                        if (method.getReturnType().toString().equals(expectedReturnType)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Close {@link Closeable}.
     *
     * @param c
     *            {@link Closeable}
     */
    public static void close(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.annotationProcessorType = processingEnv.getElementUtils()
                .getTypeElement(AnnotationProcessor.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            checkAnnotatedClasses(roundEnv);
        } else {
            if (this.generateServiceFile) {
                writeToServiceFile();
            }
        }
        return true;
    }

    /**
     * Check if classes annotated with {@link AnnotationProcessor} are build compliant.
     *
     * @param roundEnv
     *            {@link RoundEnvironment}
     */
    private void checkAnnotatedClasses(RoundEnvironment roundEnv) {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(this.annotationProcessorType);
        this.annotatedClasses = ElementFilter.typesIn(annotatedElements);
        for (TypeElement typeElement : this.annotatedClasses) {
            String superName = checkSuperclass(typeElement);
            if (superName == null) {
                return;
            }
            if (superName.equals(AbstractProcessor.class.getSimpleName())) {
                checkProcessMethod(typeElement);
            } else if (superName.equals(Processor.class.getSimpleName())) {
                checkWholeOverrideMethods(typeElement);
            }
        }
    }

    /**
     * <p>
     * Check if class extends {@link AbstractProcessor} or implements {@link Processor}.
     * </p>
     *
     * <p>
     * The Compiler throws an error if the class neither extends {@link AbstractProcessor} nor implements
     * {@link Processor}.
     * </p>
     *
     * @param typeElement
     *            {@link TypeElement} of the class
     *
     * @return
     *         <ul>
     *         <li>{@link Class#getSimpleName()} of {@link AbstractProcessor} if the class extends
     *         AbstractProcessor</li>
     *         <li>{@link Class#getSimpleName()} of {@link Processor} if the class implements Processor</li>
     *         <li>null otherwise</li>
     *         </ul>
     */
    private String checkSuperclass(TypeElement typeElement) {
        TypeMirror abstractProcessor = processingEnv.getElementUtils()
                .getTypeElement(AbstractProcessor.class.getCanonicalName()).asType();
        boolean isEAbstractProcessor = processingEnv.getTypeUtils().isSubtype(typeElement.asType(), abstractProcessor);
        if (isEAbstractProcessor) {
            return AbstractProcessor.class.getSimpleName();
        }

        TypeMirror iProcessor = processingEnv.getElementUtils().getTypeElement(Processor.class.getCanonicalName())
                .asType();
        boolean isIProcessor = processingEnv.getTypeUtils().isAssignable(typeElement.asType(), iProcessor);
        if (isIProcessor) {
            return Processor.class.getSimpleName();
        }

        this.generateServiceFile = false;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                typeElement.getQualifiedName() + " is neither extending " + AbstractProcessor.class.getCanonicalName()
                        + " nor implementing " + Processor.class.getCanonicalName() + ". Best Practise is to extend "
                        + AbstractProcessor.class.getCanonicalName() + ".",
                typeElement);
        return null;
    }

    /**
     * Check if class overrides the {@link Processor#process(Set, RoundEnvironment)} method. If the method is missing,
     * the compiler throws an error.
     *
     * @param typeElement
     *            {@link TypeElement} of the class
     *
     * @see #createMissingMethodError(TypeElement, String, List, String)
     */
    private void checkProcessMethod(TypeElement typeElement) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        List<String> expectedParamTypes = new ArrayList<>(2);
        expectedParamTypes
                .add(Set.class.getCanonicalName() + "<? extends " + TypeElement.class.getCanonicalName() + ">");
        expectedParamTypes.add(RoundEnvironment.class.getCanonicalName());
        String expectedReturnType = Boolean.TYPE.getCanonicalName();
        boolean processExists = checkIfMethodExists(methods, "process", 2, expectedParamTypes, expectedReturnType);
        if (!processExists) {
            createMissingMethodError(typeElement, "process", expectedParamTypes, expectedReturnType);
        }
    }

    /**
     * Check if class implementing {@link Processor} overrides all methods. If a method is missing, the compiler will
     * throw an error.
     *
     * @param typeElement
     *            {@link TypeElement} of the class
     *
     * @see #createMissingMethodError(TypeElement, String, List, String)
     */
    private void checkWholeOverrideMethods(TypeElement typeElement) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        List<String> expectedParamTypes = new ArrayList<>(4);
        String expectedMethodName;
        String expectedReturnValue;

        expectedMethodName = "getSupportedOptions";
        expectedReturnValue = Set.class.getCanonicalName() + "<" + String.class.getCanonicalName() + ">";
        boolean overridesSupportedOptions = checkIfMethodExists(methods, expectedMethodName, 0, null,
                expectedReturnValue);
        if (!overridesSupportedOptions) {
            createMissingMethodError(typeElement, expectedMethodName, null, expectedReturnValue);
        }

        expectedMethodName = "getSupportedAnnotationTypes";
        expectedReturnValue = Set.class.getCanonicalName() + "<" + String.class.getCanonicalName() + ">";
        boolean overridesSupportedAnnotationTypes = checkIfMethodExists(methods, expectedMethodName, 0, null,
                expectedReturnValue);
        if (!overridesSupportedAnnotationTypes) {
            createMissingMethodError(typeElement, expectedMethodName, null, expectedReturnValue);
        }

        expectedMethodName = "getSupportedSourceVersion";
        expectedReturnValue = SourceVersion.class.getCanonicalName();
        boolean overridesSupportedSourceVersion = checkIfMethodExists(methods, expectedMethodName, 0, null,
                expectedReturnValue);
        if (!overridesSupportedSourceVersion) {
            createMissingMethodError(typeElement, expectedMethodName, null, expectedReturnValue);
        }

        expectedMethodName = "init";
        expectedReturnValue = Void.TYPE.getCanonicalName();
        expectedParamTypes.add((ProcessingEnvironment.class.getCanonicalName()));
        boolean overridesInit = checkIfMethodExists(methods, expectedMethodName, 1, expectedParamTypes,
                expectedReturnValue);
        if (!overridesInit) {
            createMissingMethodError(typeElement, expectedMethodName, expectedParamTypes, expectedReturnValue);
        }

        checkProcessMethod(typeElement);

        expectedMethodName = "getCompletions";
        expectedReturnValue = Iterable.class.getCanonicalName() + "<? extends " + Completion.class.getCanonicalName()
                + ">";
        expectedParamTypes.clear();
        expectedParamTypes.add(Element.class.getCanonicalName());
        expectedParamTypes.add(AnnotationMirror.class.getCanonicalName());
        expectedParamTypes.add(ExecutableElement.class.getCanonicalName());
        expectedParamTypes.add(String.class.getCanonicalName());
        boolean overridesGetCompletions = checkIfMethodExists(methods, expectedMethodName, 4, expectedParamTypes,
                expectedReturnValue);
        if (!overridesGetCompletions) {
            createMissingMethodError(typeElement, expectedMethodName, expectedParamTypes, expectedReturnValue);
        }
    }

    /**
     * Throws a compiler error and prevents service file from being created.
     *
     * @param typeElement
     *            {@link TypeElement} of the class
     * @param methodName
     *            Name of the method not overridden
     * @param paramTypes
     *            {@link List} with parameter types of the method
     * @param returnValue
     *            {@link Class#getCanonicalName()} of the return value
     */
    private void createMissingMethodError(TypeElement typeElement, String methodName, List<String> paramTypes,
            String returnValue) {
        this.generateServiceFile = false;
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(typeElement.getQualifiedName()).append(" is not overriding ")
                .append(Processor.class.getCanonicalName()).append("#").append(methodName).append("(");
        if (paramTypes != null) {
            for (int i = 0; i < paramTypes.size(); i++) {
                errorMsg.append(paramTypes.get(i));
                if (i != paramTypes.size() - 1) {
                    errorMsg.append(", ");
                }
            }
        }
        errorMsg.append(") (ReturnType ").append(returnValue).append(").");
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, errorMsg, typeElement);
    }

    /**
     * Write canonical names of classes annotated with {@link AnnotationProcessor} to service file.
     */
    private void writeToServiceFile() {
        OutputStream outputStream;
        try {
            FileObject newServiceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    PROCESSOR_SERVICE_FILE);
            outputStream = newServiceFile.openOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (TypeElement annotatedClass : this.annotatedClasses) {
            stringBuilder.append(annotatedClass.getQualifiedName());
            stringBuilder.append(System.lineSeparator());
        }

        try {
            outputStream.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close(outputStream);
        }
    }
}
