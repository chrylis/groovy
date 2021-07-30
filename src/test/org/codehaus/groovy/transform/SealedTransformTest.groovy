/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.transform

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.junit.Test

import static groovy.test.GroovyAssert.assertScript
import static groovy.test.GroovyAssert.shouldFail

class SealedTransformTest {

    @Test
    void testSimpleSealedHierarchy() {
        assertScript '''
            import groovy.transform.Sealed
            import groovy.transform.NonSealed

            @Sealed(permittedSubclasses='Circle,Polygon,Rectangle') class Shape { }
            final class Circle extends Shape { }
            @NonSealed class Polygon extends Shape { }
            final class Pentagon extends Polygon { }
            @Sealed(permittedSubclasses='Square') class Rectangle extends Shape { }
            final class Square extends Rectangle { }

            assert [new Circle(), new Square()]*.class.name == ['Circle', 'Square']
        '''
    }

    @Test
    void testSimpleSealedHierarchyTraits() {
        assertScript '''
            import groovy.transform.Sealed

            @Sealed(permittedSubclasses='Diamond,Star') trait ShapeT { }
            final class Diamond implements ShapeT { }
            final class Star implements ShapeT { }

            assert [new Diamond(), new Star()]*.class.name == ['Diamond', 'Star']
        '''
    }

    @Test
    void testInvalidExtensionOfSealed() {
        assert shouldFail(MultipleCompilationErrorsException, '''
            import groovy.transform.Sealed

            @Sealed(permittedSubclasses='Circle') class Shape { }
            final class Circle extends Shape { }
            class Polygon extends Shape { }
        ''').message.contains("The class 'Polygon' is not a permitted subclass of the sealed class 'Shape'")
    }

    @Test
    void testFinalAndSealed() {
        assert shouldFail(MultipleCompilationErrorsException, '''
            import groovy.transform.Sealed

            final @Sealed(permittedSubclasses='Circle') class Shape { }
            final class Circle extends Shape { }
        ''').message.contains("The class 'Shape' cannot be both final and sealed")
    }

    @Test
    void testFinalAndNonSealed() {
        assert shouldFail(MultipleCompilationErrorsException, '''
            import groovy.transform.Sealed
            import groovy.transform.NonSealed

            @Sealed(permittedSubclasses='Circle') class Shape { }
            final @NonSealed class Circle extends Shape { }
        ''').message.contains("The class 'Circle' cannot be both final and non-sealed")
    }

    @Test
    void testNonSealedNoParent() {
        assert shouldFail(MultipleCompilationErrorsException, '''
            import groovy.transform.NonSealed

            @NonSealed class Shape { }
        ''').message.contains("The class 'Shape' cannot be non-sealed as it has no sealed parent")
    }

    @Test
    void testSealedAndNonSealed() {
        assert shouldFail(MultipleCompilationErrorsException, '''
            import groovy.transform.Sealed
            import groovy.transform.NonSealed

            @Sealed(permittedSubclasses='Ellipse') class Shape { }
            @Sealed(permittedSubclasses='Circle') @NonSealed class Ellipse extends Shape { }
            final class Circle extends Ellipse { }
        ''').message.contains("The class 'Ellipse' cannot be both sealed and non-sealed")
    }

    @Test
    void testInvalidNonSealed() {
        assert shouldFail(MultipleCompilationErrorsException, '''
            import groovy.transform.NonSealed

            class Shape { }
            @NonSealed class Ellipse extends Shape { }
        ''').message.contains("The class 'Ellipse' cannot be non-sealed as it has no sealed parent")
    }

    @Test
    void testInferredPermittedAuxiliaryClasses() {
        // If the base class and all subclasses appear in the same source file, the
        // permittedSubclasses list will be automatically completed if not specified explicitly.
        // If an explicit list is given, it must be the complete list and won't
        // be extended with any additional detected subclasses in the same source file.
        assert new GroovyShell().evaluate('''
            import groovy.transform.Sealed

            @Sealed class Shape { }
            final class Square extends Shape { }
            final class Circle extends Shape { }
            Shape.getAnnotation(Sealed).permittedSubclasses()
        ''') == ['Square', 'Circle']
    }

    @Test
    void testInferredPermittedAuxiliaryInterfaces() {
        assert new GroovyShell().evaluate('''
            import groovy.transform.Sealed

            @Sealed interface Shape { }
            @Sealed interface Polygon extends Shape { }
            final class Circle implements Shape { }
            final class Rectangle implements Polygon { }
            [Shape.getAnnotation(Sealed).permittedSubclasses(),
             Polygon.getAnnotation(Sealed).permittedSubclasses()]
        ''') == [['Polygon', 'Circle'], ['Rectangle']]
    }

    @Test
    void testInferredPermittedNestedClasses() {
        assert new GroovyShell().evaluate('''
            import groovy.transform.Sealed

            @Sealed class Shape {
                final class Triangle extends Shape { }
                final class Polygon extends Shape { }
            }
            Shape.getAnnotation(Sealed).permittedSubclasses()
        ''') == ['Shape$Triangle', 'Shape$Polygon']
    }
}
