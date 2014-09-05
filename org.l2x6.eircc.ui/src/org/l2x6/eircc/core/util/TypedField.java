/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface TypedField {
    public class TypedFieldData {
        protected static PropertyDescriptor createPropertyDescriptor(String fieldName, Class<?> fieldType) {
            try {
                return new PropertyDescriptor(fieldName, fieldType);
            } catch (IntrospectionException e) {
                throw new RuntimeException(e);
            }
        }

        private final PropertyDescriptor descriptor;
        private final Method valueOfMethod;

        public TypedFieldData(PropertyDescriptor descriptor) {
            this.descriptor = descriptor;
            Method valueOfMethod = null;
            Class<?> propType = descriptor.getPropertyType();
            if (propType != String.class) {
                if (propType.isPrimitive()) {
                    propType = IrcUtils.toWrapperType(propType);
                }
                try {
                    valueOfMethod = propType.getDeclaredMethod("valueOf", String.class);
                } catch (NoSuchMethodException e) {
                    /* this.valueOfMethod will be null, which is legal */
                }
            }
            this.valueOfMethod = valueOfMethod;
        }

        public TypedFieldData(String fieldName, Class<?> fieldType) {
            this(createPropertyDescriptor(fieldName, fieldType));
        }

        protected PropertyDescriptor getDescriptor() {
            return descriptor;
        }

        public Method getValueOfMethod() {
            return valueOfMethod;
        }
    }

    default Object fromString(String value) {
        if (value == null) {
            return value;
        }
        TypedFieldData fieldData = getTypedFieldData();
        if (fieldData.getDescriptor().getPropertyType().equals(String.class)) {
            return value;
        }
        try {
            Method valueOfMethod = fieldData.getValueOfMethod();
            return valueOfMethod.invoke(null, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    default Object get(Object bean) {
        try {
            return getTypedFieldData().getDescriptor().getReadMethod().invoke(bean);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    default String getString(Object bean) {
        Object result = get(bean);
        return result == null ? null : result.toString();
    }

    TypedFieldData getTypedFieldData();

    String name();

    default void set(Object bean, Object value) {
        try {
            getTypedFieldData().getDescriptor().getWriteMethod().invoke(bean, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    default void setString(Object bean, String value) {
        Object o = fromString(value);
        set(bean, o);
    }

}
