package com.plug.logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.stream.Stream;

public class LoggingDecorator<T> implements InvocationHandler {

    private final T target;
    private final String[] gatheredWipedKeys;
    private final String[] gatheredProtectedKeys;

    private static final String[] DEFAULT_WIPED_KEYS = { "password", "accessToken", "clave", "pass", "certificatePassword", "certificate", "secret", "newPassword", "Connection-string", "connection-string", "access_token", "apiKey", "Authorization" };
    private static final String[] DEFAULT_PROTECTED_KEYS = { "TD", "nroTarjeta", "track1", "track2", "cvc", "cvv2", "numero", "card_number", "security_code", "number", "numeroTarjeta" };

    /**
     * Constructor privado. Usa los métodos estáticos wrap() o builder().
     */
    private LoggingDecorator(T target, String[] wipedKeys, String[] protectedKeys) {
        this.target = target;
        this.gatheredWipedKeys = initializeKeys(wipedKeys, DEFAULT_WIPED_KEYS);
        this.gatheredProtectedKeys = initializeKeys(protectedKeys, DEFAULT_PROTECTED_KEYS);
    }

    /**
     * Envuelve un objeto con logging automático usando su interfaz.
     * @param <T> tipo del objeto
     * @param target objeto a decorar
     * @param interfaceClass interfaz que implementa el objeto
     * @return proxy del objeto con logging
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target, Class<T> interfaceClass) {
        LoggingDecorator<T> handler = new LoggingDecorator<>(target, null, null);
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[] { interfaceClass },
                handler
        );
    }

    /**
     * Envuelve un objeto con logging automático usando todas sus interfaces.
     * @param <T> tipo del objeto
     * @param target objeto a decorar
     * @return proxy del objeto con logging
     * @throws IllegalArgumentException si el objeto no implementa interfaces
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target) {
        Class<?>[] interfaces = target.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException(
                    "El objeto debe implementar al menos una interfaz para usar Dynamic Proxy. " +
                    "Usa wrap(target, Interface.class) o considera usar CGLIB."
            );
        }
        LoggingDecorator<T> handler = new LoggingDecorator<>(target, null, null);
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                interfaces,
                handler
        );
    }

    /**
     * Crea un builder para configuración avanzada.
     * @param <T> tipo del objeto
     * @return builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }

        Exception ex = null;
        Object retVal = null;
        long start = System.nanoTime();
        try {
            retVal = method.invoke(target, args);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                ex = (Exception) cause;
            } else {
                throw e;
            }
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        LogEntry logEntry = buildLogEntry(method, args, ex, duration);
        if (ex != null) {
            System.err.println(logEntry);
            throw ex;
        } else {
            System.out.println(logEntry);
        }
        return retVal;
    }

    private LogEntry buildLogEntry(Method method, Object[] args, Exception ex, long duration) {
        LogEntry logEntry = new LogEntry();
        logEntry.setClassName(target.getClass().getSimpleName());
        logEntry.setMethod(method.getName());
        if (args != null && args.length > 0) {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < args.length; i++) {
                String paramValue = processParameter(args[i], parameters[i]);
                logEntry.getParameters().add(paramValue);
            }
        }
        if (ex != null) {
            logEntry.setErrorCode(ex.getClass().getSimpleName());
        }
        logEntry.setDuration(duration);
        return logEntry;
    }

    private String processParameter(Object arg, Parameter parameter) {
        if (parameter.isAnnotationPresent(Masked.class)) {
            return SecurityUtil.protect(ObjectSerializer.serialize(arg));
        } else if (parameter.isAnnotationPresent(Protected.class)) {
            return "[WIPED]";
        } else if (parameter.isAnnotationPresent(Ignored.class)) {
            return "";
        }
        return processParameterValue(arg, parameter.getName());
    }

    private String processParameterValue(Object arg, String paramName) {
        if (arg == null) {
            return "null";
        }
        
        // Verificar si el nombre del parámetro coincide con claves sensibles
        if (gatheredWipedKeys != null && gatheredWipedKeys.length > 0) {
            for (String key : gatheredWipedKeys) {
                if (paramName.toLowerCase().contains(key.toLowerCase())) {
                    return "[WIPED]";
                }
            }
        }
        
        if (gatheredProtectedKeys != null && gatheredProtectedKeys.length > 0) {
            for (String key : gatheredProtectedKeys) {
                if (paramName.toLowerCase().contains(key.toLowerCase())) {
                    return SecurityUtil.protect(ObjectSerializer.serialize(arg));
                }
            }
        }
        
        // Serializar el objeto normalmente
        return ObjectSerializer.serialize(arg);
    }

    private String[] initializeKeys(String[] providedKeys, String[] defaultKeys) {
        return Stream.of(defaultKeys, providedKeys)
                .filter(Objects::nonNull)
                .flatMap(Stream::of)
                .distinct()
                .toArray(String[]::new);
    }

    public static class Builder<T> {
        private T target;
        private String[] wipedKeys;
        private String[] protectedKeys;
        private Class<?>[] interfaces;

        public Builder<T> target(T target) {
            this.target = target;
            return this;
        }
        public Builder<T> wipedKeys(String... keys) {
            this.wipedKeys = keys;
            return this;
        }
        public Builder<T> protectedKeys(String... keys) {
            this.protectedKeys = keys;
            return this;
        }
        public Builder<T> interfaces(Class<?>... interfaces) {
            this.interfaces = interfaces;
            return this;
        }
        @SuppressWarnings("unchecked")
        public T build() {
            if (target == null) {
                throw new IllegalStateException("Target no puede ser null");
            }
            Class<?>[] targetInterfaces = interfaces;
            if (targetInterfaces == null || targetInterfaces.length == 0) {
                targetInterfaces = target.getClass().getInterfaces();
                if (targetInterfaces.length == 0) {
                    throw new IllegalArgumentException(
                            "El objeto debe implementar al menos una interfaz o especificar interfaces explícitamente"
                    );
                }
            }
            LoggingDecorator<T> handler = new LoggingDecorator<>(target, wipedKeys, protectedKeys);
            return (T) Proxy.newProxyInstance(
                    target.getClass().getClassLoader(),
                    targetInterfaces,
                    handler
            );
        }
    }
}