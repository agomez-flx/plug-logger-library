package com.plug.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utilidad para serializar objetos a formato String legible.
 */
class ObjectSerializer {
    
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        // Configurar para formato legible
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Ignorar propiedades vacías
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }
    
    /**
     * Serializa un objeto a String.
     * Para tipos primitivos y String devuelve el toString().
     * Para objetos complejos intenta serializar a JSON.
     * 
     * @param obj objeto a serializar
     * @return representación en String del objeto
     */
    public static String serialize(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        // Para tipos primitivos y wrapper classes, usar toString()
        if (isPrimitiveOrWrapper(obj.getClass()) || obj instanceof String) {
            return obj.toString();
        }
        
        // Para objetos complejos, intentar serializar a JSON
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            // Si falla la serialización JSON, usar toString()
            return obj.toString();
        }
    }
    
    /**
     * Verifica si una clase es un tipo primitivo o su wrapper.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() 
            || type == Boolean.class
            || type == Integer.class
            || type == Long.class
            || type == Double.class
            || type == Float.class
            || type == Short.class
            || type == Byte.class
            || type == Character.class;
    }
}
