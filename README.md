# Calendario Colombia 8.0
Nueva identidad visible de la aplicación antes llamada Festivos Colombia.

## Mejoras
- Nombre Calendario Colombia en el lanzador y en la pantalla principal.
- Herramientas agrupadas en un menú superior para dar más espacio al calendario.
- Botones con respuesta visual al tocar y texto más legible.
- Navegación por deslizamiento horizontal: meses en Mensual y años en Anual; conserva flechas y rango 2000–2100.
- Notas y resultados de búsqueda con fecha destacada y vista previa separada.
- Editor de notas con letra mayor, más espacio y contador de caracteres.

Se conservan icono con bandera, modo claro/oscuro, notas, respaldo .fco, búsqueda, Ir a fecha y domingos/festivos en rojo. Sin aviso del próximo festivo.

## Compilar
Android 8.0 o posterior, JDK 17, Gradle 8.9, Android SDK 35, Android Gradle Plugin 8.7.3.
`gradle :app:assembleDebug`
APK: app/build/outputs/apk/debug/app-debug.apk.
No incluye Gradle Wrapper. El flujo incluido instala Gradle.

## Compatibilidad
applicationId y namespace siguen siendo co.calendario.festivos; MainActivity conserva su nombre para mantener preferencias. Notas date_notes, apariencia appearance y preferencia annual sin migración. El formato y encabezado de respaldo de la versión 7 se conservan deliberadamente para importar los archivos .fco anteriores. Cambia el nombre sugerido del archivo exportado a Calendario-Colombia-notas-fecha.fco.
Actualización de datos conservada si la firma de los APK coincide. Consulta LEEME-ACTUALIZACION.md.
