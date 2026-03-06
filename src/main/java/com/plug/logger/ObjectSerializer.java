package com.plug.logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utilidad para serializar objetos a formato String legible.
 */
class ObjectSerializer {
    
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        // Registrar módulo para tipos de fecha/hora de Java 8
        objectMapper.registerModule(new JavaTimeModule());
        // Configurar para formato legible sin indentación (una línea)
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        // Ignorar propiedades vacías
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // Incluir todas las propiedades, incluso nulls
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // Desactivar fechas como timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Configuraciones adicionales para ser más permisivo
        objectMapper.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        objectMapper.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
            String json = objectMapper.writeValueAsString(obj);
            return json;
        } catch (JsonProcessingException e) {
            // Si falla la serialización JSON, devolver información del error
            String errorMsg = String.format("[Serialization failed for %s: %s]", 
                obj.getClass().getName(), 
                e.getMessage());
            // Log del error para debugging
            System.err.println("ObjectSerializer error: " + errorMsg);
            e.printStackTrace();
            // Intentar con toString() como fallback
            try {
                return obj.toString();
            } catch (Exception toStringEx) {
                return errorMsg;
            }
        } catch (Exception e) {
            // Cualquier otro error
            String errorMsg = String.format("[Error serializing %s: %s]", 
                obj.getClass().getName(), 
                e.getMessage());
            System.err.println("ObjectSerializer error: " + errorMsg);
            return errorMsg;
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
