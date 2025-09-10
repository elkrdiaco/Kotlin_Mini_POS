# MiniPOS
This is an Android application.


Prueba Técnica – Desarrollador Kotlin (Mini POS)
Contexto
Se busca construir una aplicación, que funcione en dispositivos POS Android. Esta prueba tiene como
objetivo evaluar tus conocimientos en Kotlin, arquitectura limpia, MVI con Jetpack Compose, y tu
capacidad para resolver problemas con soluciones claras y mantenibles.
Objetivo de la Prueba
Construir un flujo mínimo que permita:
Consultar saldo de un cliente.
Registrar un depósito en su cuenta.
Sincronizar operaciones pendientes si no hay conexión al momento de la transacción.

Historias de Usuario
US-01 – Consultar saldo
Como cajero, ingreso RUT o N° de cuenta y veo el saldo actual.
Validar formato de identificador.
Manejo de error si el cliente no existe.
US-02 – Depositar
Como cajero, ingreso monto y confirmo el depósito.
Si hay conexión: aplicar depósito y mostrar saldo actualizado.
Si no hay conexión: encolar operación localmente para enviar luego.
US-03 – Sincronizar pendientes
Como cajero, quiero poder enviar operaciones encoladas cuando vuelva la conexión.

Reglas de negocio
Monto mínimo depósito: \$1.000 CLP.
Monto máximo: \$200.000 CLP.
Comisión fija: \$200 CLP por operación.
El estado de conexión puede simularse con un toggle en la UI.


Requisitos Técnicos
Lenguaje: Kotlin.
UI: Jetpack Compose.
Arquitectura: Clean Architecture + MVI.
DI: Hilt.
Persistencia: Room (opcional, se acepta in-memory si abstraído).
Pruebas: al menos 2 unit tests (casos de uso y reducción de estado).

Alcance esperado (60–90 min)
Pantalla de Saldo: input de identificador y botón “Consultar”.
Pantalla de Depósito: input de monto, toggle de conexión, botón “Depositar”.
Pantalla de Sincronizar: lista de operaciones pendientes y botón “Sincronizar”.

Simulación de API
Implementar un servicio simulado que:
getBalance(accountId) → retorna saldo con delay de 500–800ms.
makeDeposit(deposit) → actualiza saldo restando comisión.
Retorne errores en un 20% de los casos para validar manejo de errores.

Evaluación (100 pts)
Kotlin y Clean Architecture (25 pts)
MVI + Compose (25 pts)
DI con Hilt (10 pts)
Offline básico (20 pts)
Pruebas (10 pts)
Calidad general y README (10 pts)
Bonus (+10 pts): uso de Room, validación avanzada de RUT, test de UI en Compose.