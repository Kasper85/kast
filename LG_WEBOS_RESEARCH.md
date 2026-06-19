# LG webOS TV — Research técnico

Investigación de viabilidad para controlar una TV LG webOS desde una app Android nativa en la misma red WiFi.

Fecha: 2026-06-18

---

## 1. Qué es SSAP/webOS WebSocket

SSAP (Smart Service Access Protocol) es el protocolo proprietary que usan las TV LG webOS para recibir comandos remotos a través de WebSocket. No es un protocolo abierto documentado oficialmente por LG; ha sido caracterizado por la comunidad open source a través de ingeniería inversa y la documentación parcial de ConnectSDK.

### Cómo funciona

- La TV expone un servidor WebSocket en el puerto **3000** (sin autenticar) o **3001** (con SSL, tras pairing).
- El cliente envía mensajes JSON con la estructura:

```json
{
  "id": "00000001",
  "type": "request",
  "uri": "ssap://system.launcher/open",
  "payload": { "url": "https://example.com" }
}
```

- Los campos son: `id` (identificador único del mensaje), `type` (`register`, `request`, `subscribe`), `uri` (ruta del servicio SSAP), y `payload` (parámetros).
- La TV responde con JSON que incluye el mismo `id`, un `payload` con el resultado, y opcionalmente `subscribed: true` para suscripciones persistentes.

### Servicios SSAP relevantes para KastLG

| Servicio | URI | Función |
|----------|-----|---------|
| Registrar app | `ssap://` (register) | Pairing inicial con manifest |
| Abrir URL en navegador | `ssap://system.launcher/open` | `{"url": "https://..."}` |
| Lanzar app por ID | `ssap://system.launcher/launch` | `{"id": "netflix"}` |
| Obtener app en foreground | `ssap://com.webos.applicationManager/getForegroundAppInfo` | Suscribable |
| Control de volumen | `ssap://audio/setVolume` | `{"volume": 10}` |
| Encender/apagar TV | `ssap://system/turnOff` | Sin payload |
| Mostrar toast | `ssap://system.notifications/createToast` | `{"message": "..."}` |
| Obtener lista de apps | `ssap://com.webos.applicationManager/listLaunchPoints` | Retorna apps instaladas |
| Input socket (mouse/keys) | `ssap://com.webos.service.networkinput/getPointerInputSocket` | Socket especializado |

### Fuentes

- **hobbyquaker/lgtv2** — Documentación de todos los comandos SSAP conocidos:
  https://github.com/hobbyquaker/lgtv2
- **jareksedy/WebOSClient** — Lista completa de comandos con ejemplo de uso:
  https://github.com/jareksedy/WebOSClient
- **ConnectSDK** — WebOSTVService documenta comandos y versiones de webOS:
  https://connectsdk.com/en/latest/apis-ios/ios-webostvservice.html
- **nickoala/nickoala.github.io** — Referencia antigua (2017) sobre protocolo SSAP. **Caída** (404): el blog ya no está disponible. La información fue preservada en los forks y documentación de lgtv2.

---

## 2. Cómo funciona el pairing

El pairing es el proceso de autorización que la TV exige antes de aceptar comandos sensibles (control de input, power, launch de apps, etc.).

### Flujo de pairing

1. **El cliente se conecta** a `ws://TV_IP:3000` (puerto no SSL).
2. **Envía un `register`** con un manifest JSON que declara:
   - `pairingType`: `"PROMPT"` (el usuario acepta en la TV) o `"PIN"` (se muestra un PIN en la TV que el cliente debe ingresar).
   - `manifest.signed.appId`, `vendorId`, `permissions` (lista de permisos que la app solicita).
   - `manifest.permissions` (permisos de la sesión: `LAUNCH`, `CONTROL_POWER`, `READ_RUNNING_APPS`, etc.).
3. **La TV responde**:
   - Si hay un `client-key` previo almacenado → conexión autorizada inmediatamente.
   - Si no hay `client-key` → la TV muestra un prompt o PIN en pantalla.
4. **El usuario acepta** (PROMPT) o ingresa el PIN.
5. **La TV devuelve** un `client-key` en la respuesta.
6. **El cliente almacena** el `client-key` para futuras conexiones.

### Pairing manifest (pairing.json)

El manifest usado por lgtv2 (extraído de https://github.com/hobbyquaker/lgtv2/blob/master/pairing.json):

```json
{
  "forcePairing": false,
  "pairingType": "PROMPT",
  "manifest": {
    "manifestVersion": 1,
    "appVersion": "1.1",
    "signed": {
      "appId": "com.lge.test",
      "vendorId": "com.lge",
      "permissions": [
        "TEST_SECURE", "CONTROL_INPUT_TEXT", "CONTROL_MOUSE_AND_KEYBOARD",
        "READ_INSTALLED_APPS", "CONTROL_POWER", "READ_RUNNING_APPS",
        "WRITE_SETTINGS", "WRITE_NOTIFICATION_ALERT"
      ]
    },
    "permissions": [
      "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CLOSE",
      "CONTROL_AUDIO", "CONTROL_DISPLAY", "CONTROL_INPUT_JOYSTICK",
      "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_INPUT_TV", "CONTROL_POWER",
      "READ_APP_STATUS", "READ_CURRENT_CHANNEL", "READ_NETWORK_STATE",
      "READ_RUNNING_APPS", "READ_TV_CHANNEL_LIST", "WRITE_NOTIFICATION_TOAST",
      "READ_POWER_STATE", "CONTROL_TV_SCREEN", "CONTROL_TV_STANBY"
    ]
  }
}
```

### Pairing types

| Tipo | Comportamiento | User interaction |
|------|---------------|------------------|
| `PROMPT` | La TV muestra "¿Permitir conexión?" con botones Aceptar/Rechazar | 1 tap en el control remoto |
| `PIN` | La TV muestra un PIN de 8 dígitos; el cliente lo envía | Leer PIN + ingresarlo |

El tipo `PIN` es más seguro pero requiere que el usuario lea e ingrese el PIN. El tipo `PROMPT` es más cómodo pero menos seguro.

### Fuentes

- **lgtv2 pairing.json**: https://github.com/hobbyquaker/lgtv2/blob/master/pairing.json
- **lgtv2 index.js** (register flow): https://github.com/hobbyquaker/lgtv2/blob/master/index.js
- **WebOSClient** (PIN pairing example): https://github.com/jareksedy/WebOSClient

---

## 3. Cómo se obtiene y guarda el client-key

### Obtención

El `client-key` se obtiene automáticamente durante el primer pairing exitoso. La TV lo retorna en la respuesta al mensaje `register`:

```json
{
  "id": "00000001",
  "type": "register",
  "payload": {
    "client-key": "a1b2c3d4e5f6..."
  }
}
```

### Almacenamiento

| Plataforma | Método | Ejemplo |
|-----------|--------|---------|
| Node.js (lgtv2) | Archivo en disco | `~/.lgtv2/keyfile-TV_IP` |
| iOS (WebOSClient) | UserDefaults | `UserDefaults.standard.setValue(clientKey, forKey: "clientKey")` |
| Android (KastLG propuesto) | SharedPreferences o DataStore | `dataStore.edit { it["client_key"] = key }` |

### Reutilización

Una vez almacenado, el `client-key` se envía en cada `register` subsiguiente:

```json
{
  "type": "register",
  "payload": {
    "client-key": "a1b2c3d4e5f6..."
  }
}
```

Si el `client-key` es válido, la TV acepta la conexión sin mostrar prompt ni PIN. Si fue revocado o la TV fue reseteada de fábrica, se debe repetir el pairing completo.

### Fuentes

- **lgtv2 index.js** (saveKey function): https://github.com/hobbyquaker/lgtv2/blob/master/index.js
- **WebOSClient** (didRegister callback): https://github.com/jareksedy/WebOSClient

---

## 4. Abrir el navegador de la TV con una URL

**Sí, es posible.** El comando `ssap://system.launcher/open` abre una URL en el navegador web integrado de la TV.

### Comando exacto

```json
{
  "type": "request",
  "uri": "ssap://system.launcher/open",
  "payload": {
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
  }
}
```

### Alternativas

| Comando | Payload | Resultado |
|---------|---------|-----------|
| `ssap://system.launcher/open` | `{"url": "https://..."}` | Abre la URL en el navegador default |
| `ssap://system.launcher/launch` | `{"id": "webos.browser"}` | Lanza el navegador (sin URL específica) |
| `ssap://com.webos.applicationManager/launch` | `{"id": "webos.browser", "contentId": "https://..."}` | Lanza el navegador con contentId (no siempre funciona) |

**Recomendación**: Usar `ssap://system.launcher/open` con `{"url": "..."}`. Es el más directo y documentado.

### Verificación

- **lgtv2** documenta `system.launcher/open` como comando disponible: https://github.com/hobbyquaker/lgtv2
- **WebOSClient** no expone `open` directamente pero expone `launchApp(appId:)` que puede lanzar el navegador: https://github.com/jareksedy/WebOSClient
- **ConnectSDK** documenta `launch` como capability de WebOSTVService: https://connectsdk.com/en/latest/apis-ios/ios-webostvservice.html

### Limitación conocida

- Algunas URLs pueden no abrir correctamente si la TV no tiene soporte para el protocolo (ej: `magnet:` links).
- URLs HTTP se redirigen a HTTPS en la mayoría de los navegadores webOS modernos.
- El navegador de la TV puede no estar instalado en todos los modelos (especialmente modelos básicos o very old).

---

## 5. Comando/endpoint para launch browser

### Resumen de opciones

```
# Opción A: Abrir URL directamente (RECOMENDADA)
ssap://system.launcher/open → {"url": "https://..."}

# Opción B: Lanzar navegador sin URL
ssap://system.launcher/launch → {"id": "webos.browser"}

# Opción C: Lanzar app por ID (genérico)
ssap://com.webos.applicationManager/launch → {"id": "app.id.aquí"}
```

### Obtener lista de apps instaladas

Para confirmar el ID del navegador:

```json
{
  "type": "request",
  "uri": "ssap://com.webos.applicationManager/listLaunchPoints"
}
```

Esto retorna todas las apps instaladas con sus `id`, `title` y `iconUrl`.

---

## 6. Librerías y referencias open source

### Principales

| Librería | Lenguaje | Stars | Estado | URL |
|----------|----------|-------|--------|-----|
| **hobbyquaker/lgtv2** | Node.js | 344 | Mantenido (último commit reciente) | https://github.com/hobbyquaker/lgtv2 |
| **jareksedy/WebOSClient** | Swift | 38 | Activo (v1.5.1, Aug 2024) | https://github.com/jareksedy/WebOSClient |
| **ConnectSDK** | iOS/Android | — | Archivado parcialmente | https://connectsdk.com |
| **GermanBluefox/lgtv2** | Node.js | 5 | Fork de hobbyquaker | https://github.com/GermanBluefox/lgtv2 |
| **msloth/lgtv.js** | Node.js | — | Original, sin mantener | https://github.com/msloth/lgtv.js |

### Proyectos que usan lgtv2

- **node-red-contrib-lgtv**: Nodes para Node-RED
- **lgtv2mqtt**: Interface entre LG TV y MQTT
- **homebridge-webos-tv**: Plugin para Homebridge
- **ioBroker.lgtv**: Adapter para ioBroker

### Para Android específicamente

**No existe una librería Android nativa madura para SSAP.** Las opciones son:

1. **Implementar SSAP directamente** usando OkHttp WebSocket (ya en el proyecto KastLG). El protocolo es simple: JSON sobre WebSocket. Se puede implementar en ~200-300 líneas de Kotlin.
2. **Usar ConnectSDK** (si se actualiza). El SDK original tiene soporte Android pero está parcialmente archivado.
3. **Portar la lógica de lgtv2** a Kotlin. El `index.js` de lgtv2 tiene ~320 líneas y la lógica es directa.

### Fuentes

- Todas las URLs de la tabla anterior son enlaces verificados.
- La referencia antigua `nickoala/nickoala.github.io/_posts/2017-10-15-webos-remote.md` está **caída** (404). La información fue preservada en lgtv2 y sus forks.

---

## 7. Riesgos por versión de webOS

### Versiones conocidas

| webOS Version | Años | Segundo pantalla | Pairing PIN | Notas |
|--------------|------|-------------------|-------------|-------|
| 1.0–3.0 | 2012–2014 | Limitado | No | NetCast, no webOS real |
| 4.0 | 2014–2015 | SSAP básico | No | ConnectSDK lo documenta |
| 4.0.2 | 2015 | App-to-app | Sí (añadido) | Pairing PIN introducido |
| 5.0–6.0 | 2016–2017 | SSAP completo | Sí | Permisos más estrictos |
| 7.0–22 | 2018–2022 | SSAP estable | Sí | Modelos más populares |
| 23–24 | 2023–2024 | SSAP estable | Sí | Cambios menores en API |
| 25 | 2025 | SSAP estable | Sí | Versión más reciente |

### Riesgos específicos

1. **webOS 1.0–3.0**: Estas TVs no soportan SSAP moderno. Son TVs muy antiguas (10+ años). **Riesgo bajo**: el mercado objetivo tiene webOS 4.0+.

2. **Permisos restringidos en versiones nuevas**: Algunos permisos del manifest pueden estar restringidos en webOS 23+. Por ejemplo, `CONTROL_POWER` puede requerir que la app esté registrada en LG Seller Lounge. **Riesgo medio**: el comando `system.launcher/open` es básico y no debería estar restringido.

3. **Cambios en el manifest**: El manifest de lgtv2 usa `appId: "com.lge.test"` que es un ID de testing. LG podría bloquear IDs no registrados en firmware futuro. **Riesgo bajo a medio**: muchos proyectos open source lo usan sin problemas.

4. **SSL/TLS en modelos nuevos**: Algunos modelos webOS 22+ pueden forzar `wss://` (SSL) en el puerto 3001, lo que complica la conexión inicial. **Riesgo bajo**: el flujo de pairing maneja esto automáticamente.

5. **Descontinuación del segundo pantalla**: LG podría eliminar SSAP en modelos futuros en favor de su app ThinQ. **Riesgo bajo a corto plazo**.

### Fuentes

- **ConnectSDK webOS version history**: https://connectsdk.com/en/latest/apis-ios/ios-webostvservice.html
- **lgtv2 issues**: https://github.com/hobbyquaker/lgtv2/issues (23 issues abiertas, muchas sobre compatibilidad)

---

## 8. Limitaciones conocidas

1. **Misma red WiFi**: Tanto el celular como la TV deben estar en la misma red local. Sin acceso a Internet entre ellos.

2. **Pairing requiere interacción del usuario**: La primera conexión siempre requiere que alguien acepte en la TV (PROMPT o PIN). No se puede hacer pairing completamente silencioso.

3. **Sin documentación oficial de LG**: El protocolo SSAP es undocumented. Toda la información viene de ingeniería inversa y la comunidad.

4. **No hay librería Android oficial**: LG no provee un SDK Android para control de webOS TV. La app ThinQ usa una infraestructura cloud diferente.

5. **Descubrimiento de TV**: No hay un mecanismo automático de descubrimiento en el protocolo SSAP. Se necesita SSDP (Simple Service Discovery Protocol) o ingreso manual de IP. ConnectSDK usa SSDP para discovery.

6. **Algunos comandos requieren pairing**: Mouse control, text input, power control, y TV channel control solo funcionan tras pairing exitoso.

7. **El navegador de la TV puede no existir**: En modelos very básicos o mercados específicos, el navegador puede no estar preinstalado.

8. **Timeout de conexión**: Si la TV está apagada o en standby profundo, la conexión WebSocket falla. No hay wake-on-LAN nativo a través de SSAP (aunque `CONTROL_WOL` está en los permisos del manifest).

---

## 9. Plan recomendado para Fase 5 — Configuración TV

### Objetivos de Fase 5

- Guardar IP de la TV en persistencia local.
- Probar conexión WebSocket a la TV.
- Mostrar estado de la TV (conectada/desconectada).
- Guardar client-key tras pairing exitoso.

### Implementación recomendada

**9.1. Modelo de datos**

```
TvConfig:
  - tv_ip: String
  - tv_name: String (obtenido de SSDP o manual)
  - client_key: String? (null si no hay pairing)
  - is_paired: Boolean
```

**9.2. Descubrimiento (opcional, recomendado)**

- Usar SSDP para descubrir TVs en la red local.
- Filtro: `urn:schemas-upnp-org:device:Basic:1` o `ssdp:all`.
- Alternativa: ingreso manual de IP si SSDP no está disponible.

**9.3. Conexión WebSocket**

- Usar OkHttp WebSocket (ya en el proyecto).
- Conectar a `ws://TV_IP:3000`.
- Enviar `register` con manifest y client-key almacenado (si existe).
- Si la TV responde con `client-key` → pairing exitoso, guardar key.
- Si la TV responde sin `client-key` → emitir evento de pairing pendiente.

**9.4. UI de configuración**

- Pantalla con campo de IP de la TV.
- Botón "Conectar" que intenta la conexión.
- Si no hay pairing: mostrar prompt "Acepte la conexión en su TV" o "Ingrese el PIN".
- Guardar config tras pairing exitoso.
- Botón "Probar conexión" que envía un toast a la TV.

**9.5. Persistencia**

- Usar Room (ya en el proyecto) o DataStore para guardar la config.
- Almacenar: IP, client-key, nombre de la TV.

### Dependencias nuevas

| Dependencia | Propósito | Alternativa |
|-------------|-----------|-------------|
| OkHttp WebSocket | Conexión SSAP | Ya en el proyecto |
| NsdManager (Android) | Descubrimiento SSDP | Ingreso manual de IP |

### Estimación

- ~200-300 líneas de Kotlin para la capa de conexión SSAP.
- ~100-150 líneas para la UI de configuración.
- ~50-100 líneas para tests.

---

## 10. Plan recomendado para Fase 6 — Integración webOS

### Objetivos de Fase 6

- Completar pairing con la TV.
- Lanzar el navegador con una URL de reproducción.
- Enviar URL de streaming a la TV.

### Implementación recomendada

**10.1. Envío de URL**

```kotlin
// Después del pairing exitoso y con client-key almacenado:
webOsClient.request(
    "ssap://system.launcher/open",
    mapOf("url" to streamingUrl)
)
```

**10.2. Flujo completo "Ver en TV"**

1. Usuario selecciona película en la app.
2. Usuario pulsa "Ver en TV".
3. La app verifica que hay config de TV guardada.
4. Si no hay config → navegar a Configuración TV (Fase 5).
5. Si hay config → conectar con `ws://TV_IP:3000` usando client-key.
6. Enviar `ssap://system.launcher/open` con la URL de la película.
7. Mostrar feedback: "Enviado a TV" o "Error de conexión".

**10.3. URL de reproducción**

- TMDB no provee URLs de reproducción directa.
- Opciones realistas:
  - Abrir la página TMDB de la película en el navegador de la TV (para información).
  - Abrir una app de streaming por ID (`ssap://system.launcher/launch` con `{"id": "netflix"}` y contentId).
  - Dejar que el usuario configure su servicio de streaming preferido.

### Estimación

- ~100-150 líneas adicionales para la integración de envío de URL.
- ~50 líneas para tests.

---

## 11. Decisión técnica final

### Veredicto: **CONTINUAR con el enfoque SSAP/WebSocket**

### Justificación

1. **Es técnicamente viable**: El protocolo SSAP es simple (JSON sobre WebSocket), está bien caracterizado por la comunidad, y múltiples proyectos open source lo usan con éxito.

2. **No se necesita librería externa**: OkHttp WebSocket ya está en el proyecto. La implementación de SSAP es ~200-300 líneas de Kotlin puro, sin dependencias nuevas.

3. **El caso de uso principal funciona**: `ssap://system.launcher/open` con una URL es el comando más básico y documentado. No requiere permisos especiales más allá del pairing inicial.

4. **La compatibilidad es amplia**: webOS 4.0+ (2014 en adelante) cubre la inmensa mayoría de TVs LG en uso.

5. **No hay alternativa mejor**: La app ThinQ de LG usa infraestructura cloud y requiere cuenta LG. Chromecast no es una opción (el GOAL dice explícitamente que no está en alcance). DLNA es una alternativa pero tiene menos control y no permite lanzar el navegador.

### Riesgo residual aceptable

- El protocolo es undocumented, pero estable y usado por cientos de proyectos.
- El pairing requiere interacción del usuario, pero es un evento único por TV.
- LG podría deprecar SSAP en el futuro lejano, pero no hay señales de que eso sea inminente.

### No hacer

- No usar ConnectSDK: está archivado parcialmente y tiene dependencias pesadas.
- No implementar DLNA como alternativa: no permite lanzar el navegador.
- No crear una app webOS: el GOAL dice "app Android nativa".
- No crear LG_WEBOS_RESEARCH.md como archivo separado: este documento es el research.
