package io.github.nkaaf.annotationprocessor.annotation;

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * This annotation is used to declare an annotation processor.
 * </p>
 *
 * <p>
 * Using this annotation in your annotation processor tests that it is built compliantly and set up correctly. The
 * canonical name of the processor will be added to the service file.
 * </p>
 *
 * @author Niklas Kaaf
 * 
 * @version 1.0
 * 
 * @since 1.0
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AnnotationProcessor {
}
