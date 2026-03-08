# Problema del .exe bloqueado - SOLUCIÓN

## El problema

Cuando intentas ejecutar el `.exe` generado (`Void for Jellyfin-0.2.6.exe`), se abre brevemente y se cierra inmediatamente. Esto bloquea el archivo en Windows y impide que Gradle lo elimine durante el `clean`, causando que los builds posteriores fallen.

## Por qué pasa

1. El `.exe` incluye un JRE bundled pero puede haber incompatibilidades con el sistema
2. Cuando se ejecuta y falla, Windows mantiene el archivo "en uso" por un proceso en segundo plano
3. Gradle intenta borrar el directorio durante el clean pero no puede

## Soluciones

### Opción 1: Reiniciar el ordenador (SOLUCIÓN DEFINITIVA)

Reiniciar el ordenador liberará todos los archivos bloqueados:

```bash
# Después del reinicio
./gradlew :desktop:clean :desktop:packageExe
```

### Opción 2: Usar FORCE_CLEAN.bat

Ejecuta el script que creé:

```bash
FORCE_CLEAN.bat
```

Este script:
- Cierra todos los procesos java.exe y javaw.exe
- Intenta borrar el directorio build

### Opción 3: Usar Process Explorer (avanzado)

1. Descarga Process Explorer de Microsoft: https://learn.microsoft.com/sysinternals/downloads/process-explorer
2. Ejecuta Process Explorer como administrador
3. Busca "Void for Jellyfin-0.2.6.exe" en File > Find Handle or DLL
4. Cierra el proceso que tiene el archivo abierto
5. Ejecuta `./gradlew :desktop:clean :desktop:packageExe`

### Opción 4: Compilar sin clean (rápido)

Si no necesitas limpiar, compila directamente:

```bash
./gradlew :desktop:packageExe
```

Gradle actualizará solo lo necesario y no intentará borrar el directorio bloqueado.

## Solución temporal: START_VOID.bat

Mientras solucionas el problema del exe, puedes usar la app perfectamente con:

```bash
START_VOID.bat
```

Este script ejecuta la app usando Gradle directamente, sin empaquetar.

## Próximos pasos para arreglar el exe definitivamente

He añadido:

1. **Manejo de errores robusto** en `Main.kt` - ahora si hay un error al iniciar, mostrará un diálogo con el error en lugar de cerrarse silenciosamente
2. **VlcChecker.kt** - helper para verificar que VLC está instalado
3. **JVM args optimizados** - configuración mejorada para compatibilidad
4. **DIAGNOSTICO.bat** - script completo de diagnóstico

Después de reiniciar y compilar de nuevo, si el exe sigue fallando, el nuevo manejo de errores te mostrará un diálogo con el mensaje de error exacto, lo que nos permitirá identificar y solucionar el problema real.

## Comando para compilar después de reiniciar

```bash
# Opción 1: Limpiar y compilar
./gradlew :desktop:clean :desktop:packageExe

# Opción 2: Solo compilar (más rápido)
./gradlew :desktop:packageExe
```

El nuevo instalador será: `Void_for_Jellyfin-0.2.7.exe` (con guiones bajos en el nombre).
