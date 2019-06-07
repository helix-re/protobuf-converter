package net.badata.protobuf.converter.utils;

import com.google.protobuf.Message;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoClasses;
import net.badata.protobuf.converter.annotation.ProtoField;
import net.badata.protobuf.converter.exception.MappingException;
import net.badata.protobuf.converter.exception.WriteException;
import net.badata.protobuf.converter.inspection.DefaultValue;
import net.badata.protobuf.converter.inspection.NullValueInspector;
import net.badata.protobuf.converter.mapping.Mapper;
import net.badata.protobuf.converter.resolver.FieldResolverFactory;
import net.badata.protobuf.converter.type.TypeConverter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * Utilities for extract data stored in the annotations.
 *
 * @author jsjem
 * @author Roman Gushel
 */
public class AnnotationUtils {

    /**
     * Find {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} related to {@code protobufClass}.
     *
     * @param domainClass   Domain class annotated by {@link net.badata.protobuf.converter.annotation.ProtoClass
     *                      ProtoClass}.
     * @param protobufClass Related Protobuf message class.
     * @return Instance of {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} or null if there is
     * no relation between {@code domainClass} and {@code protobufClass}.
     */
    public static ProtoClass findProtoClass(final Class<?> domainClass, final Class<? extends Message> protobufClass) {
        if (domainClass.isAnnotationPresent(ProtoClass.class)) {
            return domainClass.getAnnotation(ProtoClass.class);
        } else if (domainClass.isAnnotationPresent(ProtoClasses.class)) {
            ProtoClasses protoClasses = domainClass.getAnnotation(ProtoClasses.class);
            for (ProtoClass protoClass : protoClasses.value()) {
                if (protobufClass.isAssignableFrom(protoClass.value())) {
                    return protoClass;
                }
            }
        }
        return null;
    }


    /**
     * Create {@link net.badata.protobuf.converter.mapping.Mapper Mapper} implementation from class specified in the
     * {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} mapper field.
     *
     * @param annotation Instance of {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} annotation.
     * @return Instance of the {@link net.badata.protobuf.converter.mapping.Mapper Mapper} interface.
     * @throws MappingException If mapper instance does not contain default constructor or default constructor not
     *                          public.
     */
    public static Mapper createMapper(final ProtoClass annotation) throws MappingException {
        try {
            return annotation.mapper().newInstance();
        } catch (InstantiationException e) {
            throw new MappingException("Default constructor not found.");
        } catch (IllegalAccessException e) {
            throw new MappingException("Make default constructor public for "
                    + annotation.mapper().getSimpleName(), e);
        }
    }

    /**
     * Create {@link net.badata.protobuf.converter.resolver.FieldResolverFactory FieldResolverFactory} implementation
     * from class specified in the
     * {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} fieldFactory field.
     *
     * @param annotation Instance of {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} annotation.
     * @return Instance of the {@link net.badata.protobuf.converter.resolver.FieldResolverFactory
     * FieldResolverFactory} interface.
     * @throws MappingException If field resolver factory implementation does not contain default constructor or
     *                          default constructor not public.
     */
    public static FieldResolverFactory createFieldFactory(final ProtoClass annotation) throws MappingException {
        Class<? extends FieldResolverFactory> fieldFactory = annotation.fieldFactory();
        try {
            return fieldFactory.newInstance();
        } catch (InstantiationException e) {
            throw new MappingException("Default constructor not found.");
        } catch (IllegalAccessException e) {
            throw new MappingException("Make default constructor public for "
                    + fieldFactory.getSimpleName(), e);
        }
    }

    /**
     * Create {@link net.badata.protobuf.converter.type.TypeConverter TypeConverter} implementation from class
     * specified in the {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} converter field.
     *
     * @param annotation Instance of {@link ProtoClass ProtoClass} annotation.
     * @param domain
     * @return Instance of the {@link net.badata.protobuf.converter.type.TypeConverter TypeConverter} interface.
     * @throws WriteException If converter class does not contain default constructor or default constructor not
     *                        public.
     */
    public static TypeConverter<?, ?> createTypeConverter(final ProtoField annotation, Object domain) throws WriteException {
        try {
            Class<? extends TypeConverter<?, ?>> converter = annotation.converter();
            if (Boolean.valueOf(annotation.useConverterConstructorWithDomainObject())) {
                Optional<Constructor<?>> domainConstructor = resolveConstructorForDomainObject(converter, domain);
                if (domainConstructor.isPresent()) {
                    return (TypeConverter<?, ?>) domainConstructor.get().newInstance(domain);
                }
            }
            return converter.newInstance();
        } catch (InstantiationException e) {
            throw new WriteException("Default constructor not found.");
        } catch (IllegalAccessException e) {
            throw new WriteException("Make default constructor public for "
                    + annotation.converter().getSimpleName(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<Constructor<?>> resolveConstructorForDomainObject(Class<? extends TypeConverter<?, ?>> converter, Object domain) {
        for (Constructor<?> constructor : converter.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1) {
                if (domain == null || parameterTypes[0].isAssignableFrom(domain.getClass())) {
                    return Optional.of(constructor);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Create {@link net.badata.protobuf.converter.inspection.NullValueInspector NullValueInspector} implementation
     * from class specified in the {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} nullValue
     * field.
     *
     * @param annotation Instance of {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} annotation.
     * @return Instance of the {@link net.badata.protobuf.converter.inspection.NullValueInspector NullValueInspector}
     * interface.
     * @throws WriteException If null value inspector class does not contain default constructor or default
     *                        constructor not public.
     */
    public static NullValueInspector createNullValueInspector(final ProtoField annotation) throws WriteException {
        try {
            return annotation.nullValue().newInstance();
        } catch (InstantiationException e) {
            throw new WriteException("Default constructor not found.");
        } catch (IllegalAccessException e) {
            throw new WriteException("Make default constructor public for "
                    + annotation.nullValue().getSimpleName(), e);
        }
    }

    /**
     * Create {@link net.badata.protobuf.converter.inspection.DefaultValue DefaultValue} implementation
     * from class specified in the {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} nullValue
     * field.
     *
     * @param annotation Instance of {@link net.badata.protobuf.converter.annotation.ProtoClass ProtoClass} annotation.
     * @return Instance of the {@link net.badata.protobuf.converter.inspection.DefaultValue DefaultValue} interface.
     * @throws WriteException If default value creator class does not contain default constructor or default
     *                        constructor is not public.
     */
    public static DefaultValue createDefaultValue(final ProtoField annotation) throws WriteException {
        try {
            return annotation.defaultValue().newInstance();
        } catch (InstantiationException e) {
            throw new WriteException("Default constructor not found.");
        } catch (IllegalAccessException e) {
            throw new WriteException("Make default constructor public for "
                    + annotation.defaultValue().getSimpleName(), e);
        }
    }
}
