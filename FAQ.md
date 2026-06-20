# Preguntas Frecuentes — Kast

## ¿Qué es Kast?

Kast es una aplicación Android de código abierto que permite buscar películas y series usando TMDB, guardar favoritos e historial localmente, y enviar contenido directamente a una TV LG webOS desde el celular.

## ¿Funciona con TVs LG?

Kast funciona con **cualquier TV LG que ejecute webOS** y soporte el protocolo SSAP. La mayoría de las TVs LG de 2014 en adelante son compatibles. No funciona con Samsung, Sony, ni otros fabricantes.

## ¿Necesito una cuenta?

**No.** Kast funciona completamente sin cuenta, login ni registro. Solo necesitas un token de TMDB (gratuito) para buscar películas.

## ¿Necesito TMDB?

**Sí.** TMDB es la fuente de datos de películas y series. Necesitas un API Read Access Token v4, que es gratuito. Puedes configurarlo desde la app en la pestaña Ajustes.

## ¿Funciona sin internet?

**Parcialmente.** Sin internet no puedes buscar películas (TMDB requiere conexión). Pero una vez que guardaste favoritos, estos permanecen en tu celular. El envío a la TV tampoco funciona sin conexión a la red local.

## ¿Tiene Chromecast?

**No.** Kast usa el protocolo SSAP nativo de LG webOS, que es más directo y rápido que Chromecast. No necesita un dispositivo Chromecast.

## ¿Tiene Android TV?

**No.** Kast es una app para teléfonos Android, no para Android TV. Está diseñada para enviar contenido desde el celular a la TV LG.

## ¿Tiene Samsung TV?

**No.** Kast es exclusivo para TVs LG webOS. No compatible con Samsung, Sony, ni otros fabricantes.

## ¿Guarda mis datos?

Kast guarda todo **localmente en tu celular** usando Room (base de datos SQLite). No hay backend, no hay cloud, no hay sincronización. Si desinstalas la app, pierdes los datos.

## ¿Es gratis?

**Sí.** Kast es open source bajo licencia MIT. No hay compras dentro de la app, no hay publicidad, no hay suscripciones.

## ¿Cómo conecto mi TV?

1. Ir a la pestaña **TV** en la app
2. Presioná **Buscar TVs** o ingresá la IP manualmente
3. Seleccioná la TV y presioná **Conectar**
4. Aceptá el permiso en la pantalla de la TV

## ¿Cómo obtengo el token de TMDB?

1. Creá una cuenta en [themoviedb.org](https://www.themoviedb.org/)
2. Andá a Settings > API
3. Generá un **API Read Access Token** (v4)
4. Abrí Kast → Ajustes → pegá el token

## ¿Puedo usar Kast sin una TV LG?

**Sí.** Puedes buscar películas, ver detalles, y guardar favoritos. El envío a la TV es una función adicional.

## ¿Qué es UnlimPlay?

UnlimPlay es el servicio que maneja la reproducción cuando envías una película a la TV. Kast envía la URL a la TV, y UnlimPlay se encarga de la reproducción.

## ¿Kast aloja contenido?

**No.** Kast es solo un navegador de metadatos (TMDB) y un controlador remoto para la TV. No almacena, transmite ni distribuye contenido multimedia.
