package io.github.poshjosh.ratelimiter.annotation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.Set;

@SupportedAnnotationTypes(RateProcessor.ANNOTATION_CLASS_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RateProcessor extends AbstractProcessor {

    public static final String ANNOTATION_CLASS_NAME = "io.github.poshjosh.ratelimiter.annotation.Rate";

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        annotations.forEach(annotation -> {

            Set<? extends Element> annotatedElements
                    = roundEnv.getElementsAnnotatedWith(annotation);

            annotatedElements.forEach(annotatedElement ->{

                Rate rate = annotatedElement.getAnnotation(Rate.class);
                final long limit = rate.permits();
                if (limit < 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Must not be negative, Rate.permits: " + limit);
                }
                final long duration = rate.duration();
                if (duration < 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Must not be negative, Rate.duration: " + duration);
                }
            });
        });

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(RateProcessor.ANNOTATION_CLASS_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_0;
    }
}
