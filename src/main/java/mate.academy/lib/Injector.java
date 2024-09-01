package mate.academy.lib;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import mate.academy.service.FileReaderService;
import mate.academy.service.ProductParser;
import mate.academy.service.ProductService;
import mate.academy.service.impl.FileReaderServiceImpl;
import mate.academy.service.impl.ProductParserImpl;
import mate.academy.service.impl.ProductServiceImpl;

public class Injector {
    private static final Injector injector = new Injector();
    private final Map<Class<?>, Class<?>> interfaceImplementations;
    private final Map<Class<?>, Object> instances = new HashMap<>();

    private Injector() {
        interfaceImplementations = Map.of(
                FileReaderService.class, FileReaderServiceImpl.class,
                ProductParser.class, ProductParserImpl.class,
                ProductService.class, ProductServiceImpl.class
        );
    }

    public static Injector getInjector() {
        return injector;
    }

    public Object getInstance(Class<?> interfaceClazz) {
        Class<?> implClass = interfaceImplementations.get(interfaceClazz);

        if (implClass == null) {
            throw new RuntimeException(
                    "No implementation found for interface " + interfaceClazz.getName());
        }

        if (!implClass.isAnnotationPresent(Component.class)) {
            throw new RuntimeException(
                    "Injection failed, missing @Component annotation on the class "
                            + implClass.getName());
        }

        if (instances.containsKey(implClass)) {
            return instances.get(implClass);
        }

        try {
            Object instance = implClass.getDeclaredConstructor().newInstance();
            instances.put(implClass, instance);

            for (Field field : implClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    Object dependency = getInstance(field.getType());
                    field.set(instance, dependency);
                }
            }

            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Injection failed, could not instantiate "
                    + implClass.getName(), e);
        }
    }
}
