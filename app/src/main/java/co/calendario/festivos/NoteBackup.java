package co.calendario.festivos;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.time.LocalDate;
import java.util.*;

/** Formato versionado propio, sin dependencia de Android. No cifra las notas. */
public final class NoteBackup {
    public static final int MAX_BYTES=16*1024*1024;
    private static final String HEADER="FESTIVOS_COLOMBIA_NOTES\t1";
    private static String utf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
    private static byte[] bytes(String text) throws CharacterCodingException {
        ByteBuffer b=StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
        byte[] result=new byte[b.remaining()]; b.get(result); return result;
    }
    private static void valid(String key,String note) {
        LocalDate date;
        try { date=LocalDate.parse(key); } catch(RuntimeException ex) { throw new IllegalArgumentException("Fecha inválida en el respaldo."); }
        if(!date.toString().equals(key)||date.getYear()<2000||date.getYear()>2100) throw new IllegalArgumentException("Solo se admiten fechas de 2000 a 2100.");
        if(note==null||note.trim().isEmpty()||note.length()>4000) throw new IllegalArgumentException("Hay una nota vacía o de más de 4000 caracteres.");
    }
    public static byte[] encode(Map<String,String> notes) {
        StringBuilder result=new StringBuilder(HEADER).append('\n');
        try {
            for(Map.Entry<String,String> entry:new TreeMap<>(notes).entrySet()) {
                valid(entry.getKey(),entry.getValue());
                result.append(entry.getKey()).append('\t').append(Base64.getEncoder().encodeToString(bytes(entry.getValue()))).append('\n');
                if(result.length()>MAX_BYTES) throw new IllegalArgumentException("El respaldo supera 16 MB.");
            }
            byte[] encoded=bytes(result.toString());
            if(encoded.length>MAX_BYTES) throw new IllegalArgumentException("El respaldo supera 16 MB.");
            return encoded;
        } catch(CharacterCodingException ex) { throw new IllegalArgumentException("Una nota contiene texto no válido."); }
    }
    public static Map<String,String> decode(byte[] data) {
        if(data.length>MAX_BYTES) throw new IllegalArgumentException("El archivo supera 16 MB.");
        try {
            String[] lines=utf8(data).split("\\r?\\n",-1);
            if(lines.length==0||!HEADER.equals(lines[0])) throw new IllegalArgumentException("No es un respaldo compatible de Festivos Colombia.");
            Map<String,String> result=new TreeMap<>();
            for(int i=1;i<lines.length;i++) {
                if(i==lines.length-1&&lines[i].isEmpty()) continue;
                int separator=lines[i].indexOf('\t');
                if(separator!=10||lines[i].lastIndexOf('\t')!=separator) throw new IllegalArgumentException("El respaldo tiene una línea inválida.");
                String key=lines[i].substring(0,separator);
                String value=utf8(Base64.getDecoder().decode(lines[i].substring(separator+1)));
                valid(key,value);
                if(result.put(key,value)!=null) throw new IllegalArgumentException("El respaldo tiene fechas duplicadas.");
            }
            return result;
        } catch(CharacterCodingException ex) { throw new IllegalArgumentException("El respaldo contiene texto UTF-8 no válido."); }
    }
    public static Map<String,String> merge(Map<String,String> existing,Map<String,String> incoming,boolean replaceConflicts) {
        Map<String,String> result=new TreeMap<>(existing);
        for(Map.Entry<String,String> e:incoming.entrySet()) {
            valid(e.getKey(),e.getValue());
            if(replaceConflicts||!result.containsKey(e.getKey())) result.put(e.getKey(),e.getValue());
        }
        return result;
    }
}
