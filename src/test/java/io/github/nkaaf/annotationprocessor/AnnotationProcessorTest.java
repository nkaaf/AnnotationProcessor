package io.github.nkaaf.annotationprocessor;

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

import io.github.nkaaf.annotationprocessor.processor.AnnotationProcessorProcessor;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationProcessorTest {

    private static final String _src_dir = System.getProperty("src.dir");
    private static final String _classes_dir = System.getProperty("out.dir");

    private static final String COMPILER_PROCESSOR_ERROR_CODE = "compiler.err.proc.messager";

    private static final String PACKAGE_NAME = "io.github.nkaaf.annotationprocessor.";
    private static final String PACKAGE_PATH = PACKAGE_NAME.replace('.', '/');

    private static final String INCORRECT_ABSTRACT_PROCESSOR = "IncorrectAbstractProcessor";
    private static final String INCORRECT_PROCESSOR_SUPPORTED_OPTIONS = "IncorrectProcessorSupportedOptions";
    private static final String INCORRECT_PROCESSOR_SUPPORTED_ANNOTATION_TYPES = "IncorrectProcessorSupportedAnnotationTypes";
    private static final String INCORRECT_PROCESSOR_SUPPORTED_SOURCE_VERSION = "IncorrectProcessorSupportedSourceVersion";
    private static final String INCORRECT_PROCESSOR_INIT = "IncorrectProcessorInit";
    private static final String INCORRECT_PROCESSOR_PROCESS = "IncorrectProcessorProcess";
    private static final String INCORRECT_PROCESSOR_GET_COMPLETIONS = "IncorrectProcessorGetCompletions";
    private static final String CORRECT_PROCESSOR = "CorrectProcessor";
    private static final String CORRECT_ABSTRACT_PROCESSOR = "CorrectAbstractProcessor";

    private static final String PROCESSOR_SERVICE_FILE = _classes_dir + "/META-INF/services/" + Processor.class.getCanonicalName();

    private static final List<String> OPTIONS = Arrays.asList(
            "-d", _classes_dir,
            "-processor", AnnotationProcessorProcessor.class.getCanonicalName(),
            "-classpath", _classes_dir,
            "-s", _classes_dir
    );

    private static String getJavaFile(String file) {
        return _src_dir + "/test/java/" + PACKAGE_PATH + file + ".java";
    }

    private static String getClassFile(String file) {
        return _classes_dir + "/" + PACKAGE_PATH + file + ".class";
    }

    private static String getCanonicalName(String file) {
        return PACKAGE_NAME + file;
    }

    private static boolean compile(List<String> sources, DiagnosticCollector<JavaFileObject> diagnosticCollector) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromStrings(sources);
        boolean success = compiler.getTask(null, fileManager, diagnosticCollector, OPTIONS, null, units).call();
        fileManager.close();
        return success;
    }

    private static String getErrorMessage(String classname, ErrorMessage errorMessage) {
        String error = "";
        switch (errorMessage) {
            case SUPPORTED_OPTIONS:
                error = classname + " is not overriding javax.annotation.processing.Processor#getSupportedOptions() (ReturnType java.util.Set<java.lang.String>).";
                break;
            case SUPPORTED_ANNOTATION_TYPES:
                error = classname + " is not overriding javax.annotation.processing.Processor#getSupportedAnnotationTypes() (ReturnType java.util.Set<java.lang.String>).";
                break;
            case SUPPORTED_SOURCE_VERSION:
                error = classname + " is not overriding javax.annotation.processing.Processor#getSupportedSourceVersion() (ReturnType javax.lang.model.SourceVersion).";
                break;
            case INIT:
                error = classname + " is not overriding javax.annotation.processing.Processor#init(javax.annotation.processing.ProcessingEnvironment) (ReturnType void).";
                break;
            case PROCESS:
                error = classname + " is not overriding javax.annotation.processing.Processor#process(java.util.Set<? extends javax.lang.model.element.TypeElement>, javax.annotation.processing.RoundEnvironment) (ReturnType boolean).";
                break;
            case GET_COMPLETIONS:
                error = classname + " is not overriding javax.annotation.processing.Processor#getCompletions(javax.lang.model.element.Element, javax.lang.model.element.AnnotationMirror, javax.lang.model.element.ExecutableElement, java.lang.String) (ReturnType java.lang.Iterable<? extends javax.annotation.processing.Completion>).";
        }
        return error;
    }

    @Test
    public void compileIncorrectAbstractProcessor() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(INCORRECT_ABSTRACT_PROCESSOR));

        assertFalse(compile(sources, diagnostics));
        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertFalse(processorServiceFile.exists());

        assertEquals(1, diagnostics.getDiagnostics().size());
        Diagnostic<?> diagnostic = diagnostics.getDiagnostics().get(0);
        assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind());
        assertTrue(diagnostic.getSource().toString().contains(INCORRECT_ABSTRACT_PROCESSOR));
        assertEquals(COMPILER_PROCESSOR_ERROR_CODE, diagnostic.getCode());
        assertEquals(getErrorMessage(getCanonicalName(INCORRECT_ABSTRACT_PROCESSOR), ErrorMessage.PROCESS), diagnostic.getMessage(null));
    }

    @Test
    public void compileCorrectAbstractProcessor() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(CORRECT_ABSTRACT_PROCESSOR));

        assertTrue(compile(sources, diagnostics));

        assertEquals(0, diagnostics.getDiagnostics().size());

        File correctFile = new File(getClassFile(CORRECT_ABSTRACT_PROCESSOR));
        assertTrue(correctFile.exists());

        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertTrue(processorServiceFile.exists());

        Scanner scanner = new Scanner(processorServiceFile);
        assertTrue(scanner.hasNextLine());
        assertEquals(getCanonicalName(CORRECT_ABSTRACT_PROCESSOR), scanner.nextLine());
        assertFalse(scanner.hasNextLine());

        assertTrue(correctFile.delete());
        assertTrue(processorServiceFile.delete());
    }

    @Test
    public void compileIncorrectProcessorSupportedOptions() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(INCORRECT_PROCESSOR_SUPPORTED_OPTIONS));

        assertFalse(compile(sources, diagnostics));
        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertFalse(processorServiceFile.exists());

        assertEquals(1, diagnostics.getDiagnostics().size());
        Diagnostic<?> diagnostic = diagnostics.getDiagnostics().get(0);
        assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind());
        assertTrue(diagnostic.getSource().toString().contains(INCORRECT_PROCESSOR_SUPPORTED_OPTIONS));
        assertEquals(COMPILER_PROCESSOR_ERROR_CODE, diagnostic.getCode());
        assertEquals(getErrorMessage(getCanonicalName(INCORRECT_PROCESSOR_SUPPORTED_OPTIONS), ErrorMessage.SUPPORTED_OPTIONS), diagnostic.getMessage(null));
    }

    @Test
    public void compileIncorrectProcessorSupportedAnnotationTypes() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(INCORRECT_PROCESSOR_SUPPORTED_ANNOTATION_TYPES));

        assertFalse(compile(sources, diagnostics));
        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertFalse(processorServiceFile.exists());

        assertEquals(1, diagnostics.getDiagnostics().size());
        Diagnostic<?> diagnostic = diagnostics.getDiagnostics().get(0);
        assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind());
        assertTrue(diagnostic.getSource().toString().contains(INCORRECT_PROCESSOR_SUPPORTED_ANNOTATION_TYPES));
        assertEquals(COMPILER_PROCESSOR_ERROR_CODE, diagnostic.getCode());
        assertEquals(getErrorMessage(getCanonicalName(INCORRECT_PROCESSOR_SUPPORTED_ANNOTATION_TYPES), ErrorMessage.SUPPORTED_ANNOTATION_TYPES), diagnostic.getMessage(null));
    }

    @Test
    public void compileIncorrectProcessorSourceVersion() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(INCORRECT_PROCESSOR_SUPPORTED_SOURCE_VERSION));

        assertFalse(compile(sources, diagnostics));
        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertFalse(processorServiceFile.exists());

        assertEquals(1, diagnostics.getDiagnostics().size());
        Diagnostic<?> diagnostic = diagnostics.getDiagnostics().get(0);
        assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind());
        assertTrue(diagnostic.getSource().toString().contains(INCORRECT_PROCESSOR_SUPPORTED_SOURCE_VERSION));
        assertEquals(COMPILER_PROCESSOR_ERROR_CODE, diagnostic.getCode());
        assertEquals(getErrorMessage(getCanonicalName(INCORRECT_PROCESSOR_SUPPORTED_SOURCE_VERSION), ErrorMessage.SUPPORTED_SOURCE_VERSION), diagnostic.getMessage(null));
    }

    @Test
    public void compileIncorrectProcessorInit() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(INCORRECT_PROCESSOR_INIT));

        assertFalse(compile(sources, diagnostics));
        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertFalse(processorServiceFile.exists());

        assertEquals(1, diagnostics.getDiagnostics().size());
        Diagnostic<?> diagnostic = diagnostics.getDiagnostics().get(0);
        assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind());
        assertTrue(diagnostic.getSource().toString().contains(INCORRECT_PROCESSOR_INIT));
        assertEquals(COMPILER_PROCESSOR_ERROR_CODE, diagnostic.getCode());
        assertEquals(getErrorMessage(getCanonicalName(INCORRECT_PROCESSOR_INIT), ErrorMessage.INIT), diagnostic.getMessage(null));
    }

    @Test
    public void compileIncorrectProcessorProccess() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(INCORRECT_PROCESSOR_PROCESS));

        assertFalse(compile(sources, diagnostics));
        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertFalse(processorServiceFile.exists());

        assertEquals(1, diagnostics.getDiagnostics().size());
        Diagnostic<?> diagnostic = diagnostics.getDiagnostics().get(0);
        assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind());
        assertTrue(diagnostic.getSource().toString().contains(INCORRECT_PROCESSOR_PROCESS));
        assertEquals(COMPILER_PROCESSOR_ERROR_CODE, diagnostic.getCode());
        assertEquals(getErrorMessage(getCanonicalName(INCORRECT_PROCESSOR_PROCESS), ErrorMessage.PROCESS), diagnostic.getMessage(null));
    }

    @Test
    public void compileIncorrectProcessorGetCompletions() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(INCORRECT_PROCESSOR_GET_COMPLETIONS));

        assertFalse(compile(sources, diagnostics));
        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertFalse(processorServiceFile.exists());

        assertEquals(1, diagnostics.getDiagnostics().size());
        Diagnostic<?> diagnostic = diagnostics.getDiagnostics().get(0);
        assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind());
        assertTrue(diagnostic.getSource().toString().contains(INCORRECT_PROCESSOR_GET_COMPLETIONS));
        assertEquals(COMPILER_PROCESSOR_ERROR_CODE, diagnostic.getCode());
        assertEquals(getErrorMessage(getCanonicalName(INCORRECT_PROCESSOR_GET_COMPLETIONS), ErrorMessage.GET_COMPLETIONS), diagnostic.getMessage(null));
    }

    @Test
    public void compileCorrectProcessor() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Collections.singletonList(getJavaFile(CORRECT_PROCESSOR));

        assertTrue(compile(sources, diagnostics));

        assertEquals(0, diagnostics.getDiagnostics().size());

        File correctFile = new File(getClassFile(CORRECT_PROCESSOR));
        assertTrue(correctFile.exists());

        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertTrue(processorServiceFile.exists());

        Scanner scanner = new Scanner(processorServiceFile);
        assertTrue(scanner.hasNextLine());
        assertEquals(getCanonicalName(CORRECT_PROCESSOR), scanner.nextLine());
        assertFalse(scanner.hasNextLine());

        assertTrue(correctFile.delete());
        assertTrue(processorServiceFile.delete());
    }

    @Test
    public void compileCorrectFilesAndCheckServiceFile() throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> sources = Arrays.asList(getJavaFile(CORRECT_PROCESSOR), getJavaFile(CORRECT_ABSTRACT_PROCESSOR));

        assertTrue(compile(sources, diagnostics));

        File correctFile1 = new File(getClassFile(CORRECT_PROCESSOR));
        assertTrue(correctFile1.exists());
        File correctFile2 = new File(getClassFile(CORRECT_ABSTRACT_PROCESSOR));
        assertTrue(correctFile2.exists());

        assertEquals(0, diagnostics.getDiagnostics().size());

        File processorServiceFile = new File(PROCESSOR_SERVICE_FILE);
        assertTrue(processorServiceFile.exists());

        Scanner scanner = new Scanner(processorServiceFile);
        assertTrue(scanner.hasNextLine());
        assertEquals(getCanonicalName(CORRECT_PROCESSOR), scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(getCanonicalName(CORRECT_ABSTRACT_PROCESSOR), scanner.nextLine());
        assertFalse(scanner.hasNextLine());

        assertTrue(correctFile1.delete());
        assertTrue(correctFile2.delete());
        assertTrue(processorServiceFile.delete());
    }

    private enum ErrorMessage {
        SUPPORTED_OPTIONS, SUPPORTED_ANNOTATION_TYPES, SUPPORTED_SOURCE_VERSION, INIT, PROCESS, GET_COMPLETIONS
    }
}
