# Plug Logging Library

Una librería de logging automático para Java 21 que utiliza proxies dinámicos para interceptar y registrar llamadas a métodos con protección avanzada de datos sensibles.

## 🎯 Características

- **Logging Automático**: Registra automáticamente todas las llamadas a métodos sin necesidad de código repetitivo
- **Medición de Performance**: Captura el tiempo de ejecución de cada método
- **Protección de Datos Sensibles**: Tres estrategias para proteger información confidencial:
  - `@Masked`: Aplica hash SHA-256 a valores sensibles
  - `@Protected`: Oculta completamente el valor (muestra `[WIPED]`)
  - `@Ignored`: Omite el parámetro del log
- **Detección Automática**: Identifica automáticamente datos sensibles por nombre de campo/parámetro
- **Configuración Flexible**: Permite definir listas personalizadas de claves sensibles
- **Sin Dependencias Externas**: Solo requiere Java 21 y SLF4J

## 📋 Requisitos

- Java 21 o superior
- Maven 3.6+

## 📦 Instalación

Agrega la siguiente dependencia a tu `pom.xml`:

```xml
<dependency>
    <groupId>com.plug</groupId>
    <artifactId>plug-logging-library</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 🚀 Uso Básico

### Envolver una Implementación

```java
import ar.com.plug.ld.LoggingDecorator;

// Implementación original
UserService userServiceImpl = new UserServiceImpl();

// Envolver con logging automático
UserService userService = LoggingDecorator.wrap(userServiceImpl, UserService.class);

// Todas las llamadas ahora se registran automáticamente
userService.createUser("john@example.com", "password123");
```

### Uso con Múltiples Interfaces

```java
// Si el objeto implementa múltiples interfaces, se pueden usar todas
PaymentProcessor processor = new PaymentProcessorImpl();
PaymentProcessor wrappedProcessor = LoggingDecorator.wrap(processor);
```

## 🔐 Protección de Datos Sensibles

### Usando Anotaciones

```java
import ar.com.plug.ld.*;

public interface UserService {
    // @Protected oculta completamente el valor
    void login(String username, @Protected String password);
    
    // @Masked aplica hash SHA-256
    void updateToken(@Masked String accessToken);
    
    // @Ignored omite el parámetro del log
    void logActivity(@Ignored byte[] rawData);
}
```

### Detección Automática

La librería detecta automáticamente campos sensibles por nombre:

**Claves Protegidas (Protected - Mostradas como `[WIPED]`):**
- `password`, `clave`, `pass`, `secret`, `apiKey`
- `accessToken`, `access_token`, `Authorization`
- `certificatePassword`, `certificate`
- `Connection-string`, `connection-string`
- Y más...

**Claves Enmascaradas (Masked - Hash SHA-256):**
- `TD`, `nroTarjeta`, `numeroTarjeta`
- `track1`, `track2`, `numero`, `number`
- `cvc`, `cvv2`, `security_code`
- `card_number`
- Y más...

### Configuración Personalizada

```java
import ar.com.plug.ld.LoggingDecorator;

UserService userService = LoggingDecorator.<UserService>builder()
    .target(new UserServiceImpl())
    .wipedKeys("customSecret", "internalKey")      // Claves adicionales para [WIPED]
    .protectedKeys("customCardField", "sensitive") // Claves adicionales para hash
    .build();
```

## 📊 Formato de Salida

Los logs se imprimen en el siguiente formato:

```
LogEntry|---------------------------|ClassName: UserServiceImpl|Method: createUser|Parameters: john@example.com,[WIPED]|ErrorCode: null|Duration: 45ms|---------------------------
```

En caso de error:

```
LogEntry|---------------------------|ClassName: PaymentProcessor|Method: processPayment|Parameters: 1234,500.00|ErrorCode: InsufficientFundsException|Duration: 120ms|---------------------------
```

## 🏗️ Estructura del Proyecto

```
plug-logging-library/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   └── java/
    │       └── ar/com/plug/ld/
    │           ├── LoggingDecorator.java  # Clase principal del decorador
    │           ├── LogEntry.java          # Representación de entrada de log
    │           ├── SecurityUtil.java      # Utilidades de seguridad (hash)
    │           ├── Masked.java            # Anotación para hash SHA-256
    │           ├── Protected.java         # Anotación para ocultar valor
    │           └── Ignored.java           # Anotación para ignorar parámetro
    └── test/
        └── java/
            └── ar/com/plug/ld/
                └── PlugLoggerTest.java    # Tests unitarios
```

## 🛠️ Construcción y Testing

### Compilar el proyecto

```bash
mvn clean install
```

### Ejecutar tests

```bash
mvn test
```

### Generar JAR

```bash
mvn package
```

El JAR se generará en `target/plug-logging-library-1.0.0-SNAPSHOT.jar`

## 💡 Ejemplos Avanzados

### Ejemplo con Builder Completo

```java
PaymentGateway gateway = LoggingDecorator.<PaymentGateway>builder()
    .target(new StripeGateway())
    .wipedKeys("apiSecret", "webhookSecret")
    .protectedKeys("cardToken", "customerId")
    .interfaces(PaymentGateway.class, Auditable.class)
    .build();
```

### Ejemplo de Interfaz con Anotaciones

```java
public interface PaymentService {
    /**
     * Procesa un pago con tarjeta
     * @param cardNumber número de tarjeta (se enmascarará automáticamente)
     * @param cvv código de seguridad (se ocultará)
     * @param amount monto del pago
     */
    void processPayment(@Masked String cardNumber, 
                       @Protected String cvv, 
                       double amount);
    
    /**
     * Actualiza credenciales
     * @param apiKey se ocultará completamente
     * @param metadata datos adicionales que se ignoran en logs
     */
    void updateCredentials(@Protected String apiKey, 
                          @Ignored Map<String, Object> metadata);
}
```

## 🔍 Cómo Funciona

La librería utiliza el patrón **Proxy Dinámico** de Java para:

1. Interceptar todas las llamadas a métodos de las interfaces
2. Registrar el tiempo de inicio
3. Ejecutar el método original
4. Capturar excepciones si ocurren
5. Calcular la duración de ejecución
6. Procesar y proteger parámetros sensibles
7. Generar y registrar la entrada de log

Todo esto ocurre de forma transparente sin modificar el código original.

## ⚠️ Consideraciones

- **Solo funciona con interfaces**: Los objetos a decorar deben implementar al menos una interfaz debido al uso de proxies dinámicos
- **Performance**: Hay un overhead mínimo por el procesamiento de logging (típicamente < 1ms)
- **Thread-Safety**: El decorador es thread-safe para lectura de configuración
- **Salida estándar**: Actualmente imprime a `System.out` y `System.err`. Puedes integrarlo con tu framework de logging preferido

## 📝 Dependencias

```xml
<dependencies>
    <!-- SLF4J API -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.12</version>
    </dependency>

    <!-- JUnit 5 (para testing) -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 📄 Licencia

[Especificar licencia]

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📧 Contacto

[Especificar información de contacto]

---

**Nota**: Esta es una librería en desarrollo. Para uso en producción, considera agregar:
- Integración con frameworks de logging (Log4j2, Logback)
- Soporte para logging asíncrono
- Configuración mediante archivos externos
- Métricas y monitoreo
- Soporte para CGLIB para clases sin interfaces