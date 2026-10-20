# Calendario Colombia 8.0

## Nombre y mejoras
La aplicación ahora se llama Calendario Colombia. Mantiene el icono de la bandera y todas las funciones de la versión 7, sin contador del próximo festivo.
Buscar, Ir a fecha, Mis notas, Respaldo y cambio de modo claro/oscuro están en el menú superior ☰, con descripción accesible Herramientas. La pantalla principal dedica más espacio al calendario.
Puedes usar flechas o deslizar horizontalmente en el área del calendario para cambiar de mes en Mensual o de año en Anual. Los gestos verticales conservan el desplazamiento normal. Los gestos cortos, lentos o con varios dedos no cambian la fecha. Se mantiene el rango 2000–2100.
Mis notas y Buscar separan fecha/tipo y vista previa del texto; el editor tiene letra mayor y contador de caracteres hasta 4000. Se conserva el modo oscuro y la preferencia de vista.

## Actualizar GitHub sin cambiar el flujo que ya funciona
1. Antes de instalar una nueva versión, en la app actual exporta tus notas desde Respaldo.
2. Descomprime este ZIP en una carpeta nueva.
3. En el repositorio abre Code > Add file > Upload files y sube la carpeta app completa a la raíz. Debe quedar app/build.gradle y app/src/main/…, sin otra carpeta adicional encima de app.
4. Guarda con Commit changes. Usa el flujo de Actions existente para generar el APK. Si mantiene el nombre de la versión anterior, no afecta la app: el APK compilado indicará versión 8.0 y Calendario Colombia.
5. Descarga el artefacto cuando termine en verde y extrae app-debug.apk.
6. Si también quieres actualizar el nombre del proyecto, sube settings.gradle. El identificador de instalación no cambia.

Opcionalmente puedes subir el proyecto completo, incluida la carpeta checks, y reemplazar .github/workflows/android.yml por COMPILAR-APK.yml. Este flujo ejecuta las pruebas y publica Calendario-Colombia-v8-APK. No crees un segundo flujo si ya existe uno válido.

## Notas, preferencias y firma
El identificador de aplicación y el nombre de la Activity se conservan; no se cambian claves de notas, apariencia ni vista. Las copias .fco de la versión 7 siguen siendo compatibles.
El APK solo actualiza una instalación conservando sus datos si tiene la misma firma que la versión instalada. Los servidores de GitHub pueden generar otra clave debug. Si Android rechaza la actualización, conserva el respaldo antes de desinstalar: desinstalar borra las notas y preferencias. Tras instalar puedes importar el archivo .fco desde Herramientas > Respaldo.
El nuevo nombre visible puede tardar en reflejarse en un acceso directo antiguo; vuelve a crearlo desde el cajón de aplicaciones si es necesario, sin desinstalar para cambiar solo el acceso directo.

## Validaciones realizadas
Pruebas Java aprobadas de navegación mensual y anual, límites 2000–2100, cambio de año, meses bisiestos y clasificación de gestos horizontal/vertical/diagonal/corto/lento y por densidad. Se ejecutaron nuevamente las pruebas de festivos, respaldo e importación Unicode, límites y coincidencias, y búsquedas de la versión 7. Se revisaron sintaxis Java y XML/YAML. También se comprobó que no cambiaron los archivos de lógica del respaldo y búsqueda ni el identificador de instalación.
No se compiló el APK ni se ejecutó la interfaz Android en este entorno. La compilación y revisión en tu Samsung Galaxy A24 con Android 16 quedan pendientes para esta versión 8. El funcionamiento confirmado anteriormente corresponde a la versión previa.
Prueba en el móvil: nombre en el lanzador, acceso al menú, modo oscuro, vistas, gestos horizontales y verticales, notas existentes, búsqueda y recuperación de una copia .fco de la versión 7.

Este ZIP contiene el proyecto fuente, no un APK precompilado.
