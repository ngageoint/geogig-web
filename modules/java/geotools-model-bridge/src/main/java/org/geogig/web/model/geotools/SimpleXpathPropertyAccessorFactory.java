/* Copyright (c) 2017 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.geogig.web.model.geotools;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.geotools.factory.Hints;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.util.Converters;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 *
 */
public class SimpleXpathPropertyAccessorFactory implements PropertyAccessorFactory {

    static final SimpleXpathPropertyAccessor INSTANCE = new SimpleXpathPropertyAccessor();

    public @Override PropertyAccessor createPropertyAccessor(Class<?> type, String xpath,
            Class<?> target, Hints hints) {
        return !Strings.isNullOrEmpty(xpath) && xpath.indexOf('/') > -1 ? INSTANCE : null;
    }

    static class SimpleXpathPropertyAccessor implements PropertyAccessor {

        public @Override boolean canHandle(Object object, String xpath, Class<?> target) {
            return object instanceof SimpleFeature || object instanceof SimpleFeatureType;
        }

        public @Override <T> T get(Object object, String xpath, @Nullable Class<T> target)
                throws IllegalArgumentException {

            final List<String> path = Splitter.on('/').splitToList(xpath);
            if (object instanceof SimpleFeature) {
                ComplexAttribute f = (ComplexAttribute) object;
                T value = null;
                for (String step : path) {
                    Property attribute = f.getProperty(step);
                    if (attribute == null) {
                        break;
                    }
                    Object attValue = attribute.getValue();
                    if (attValue instanceof ComplexAttribute) {
                        f = (ComplexAttribute) attValue;
                    } else {
                        value = Converters.convert(value, target);
                    }
                }
                return value;
            }
            if (object instanceof SimpleFeatureType) {
                ComplexType type = (ComplexType) object;
                for (int i = 0; i < path.size() - 1; i++) {
                    String step = path.get(i);
                    PropertyDescriptor descriptor = type.getDescriptor(step);
                    if (descriptor == null) {
                        return null;
                    }
                    PropertyType propertyType = descriptor.getType();
                    if (propertyType instanceof ComplexType) {
                        type = (ComplexType) propertyType;
                    } else {
                        return null;
                    }
                }
                PropertyDescriptor result = type.getDescriptor(path.get(path.size() - 1));
                return (T) result;
            }
            return null;
        }

        public @Override <T> void set(Object object, String xpath, T value, Class<T> target)
                throws IllegalArgumentException {

            throw new UnsupportedOperationException();
        }

    }
}
