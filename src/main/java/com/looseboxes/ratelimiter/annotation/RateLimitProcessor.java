package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.Rate;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.Set;

@SupportedAnnotationTypes(RateLimitProcessor.ANNOTATION_CLASS_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RateLimitProcessor extends AbstractProcessor {

    public static final String ANNOTATION_CLASS_NAME = "com.looseboxes.ratelimiter.annotations.Rate";

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
                    messager.printMessage(Diagnostic.Kind.ERROR, "Must not be negative, permits: " + limit);
                }
                final long duration = rate.duration();
                if (duration < 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Must not be negative, duration: " + duration);
                }
            });
        });

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(RateLimitProcessor.ANNOTATION_CLASS_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_0;
    }
}
