/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import junit.framework.TestCase;

public final class AnnotationsTest extends TestCase {

    public void testDirectAnnotations() {
        Annotation[] annotations = HasAnnotations.class.getAnnotations();
        assertEquals(2, annotations.length);
        assertContains(annotations, AnnotationA.class);
        assertContains(annotations, AnnotationB.class);

        assertNotNull(HasAnnotations.class.getAnnotation(AnnotationA.class));
        assertTrue(HasAnnotations.class.isAnnotationPresent(AnnotationA.class));
        assertNotNull(HasAnnotations.class.getAnnotation(AnnotationB.class));
        assertTrue(HasAnnotations.class.isAnnotationPresent(AnnotationB.class));
        assertNull(HasAnnotations.class.getAnnotation(AnnotationC.class));
        assertFalse(HasAnnotations.class.isAnnotationPresent(AnnotationC.class));
    }

    public void testInheritedAnnotations() {
        Annotation[] annotations = ExtendsHasAnnotations.class.getAnnotations();
        assertEquals(1, annotations.length);
        assertContains(annotations, AnnotationB.class);

        assertNull(ExtendsHasAnnotations.class.getAnnotation(AnnotationA.class));
        assertFalse(ExtendsHasAnnotations.class.isAnnotationPresent(AnnotationA.class));
        assertNotNull(ExtendsHasAnnotations.class.getAnnotation(AnnotationB.class));
        assertTrue(ExtendsHasAnnotations.class.isAnnotationPresent(AnnotationB.class));
        assertNull(ExtendsHasAnnotations.class.getAnnotation(AnnotationC.class));
        assertFalse(ExtendsHasAnnotations.class.isAnnotationPresent(AnnotationC.class));
    }

    @AnnotationA
    @AnnotationB
    static class HasAnnotations {}

    static class ExtendsHasAnnotations extends HasAnnotations {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationA {}

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationB {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationC {}

    private void assertContains(Annotation[] annotations, Class<? extends Annotation> type) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == type) {
                return;
            }
        }
        fail();
    }
}
