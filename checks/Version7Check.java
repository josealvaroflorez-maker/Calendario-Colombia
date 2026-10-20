package co.calendario.festivos;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class Version7Check {
    private static void require(boolean condition,String label) {if(!condition)throw new AssertionError(label);}
    private static void reject(byte[] file,String label) {
        try {NoteBackup.decode(file);throw new AssertionError("Se aceptó "+label);}catch(IllegalArgumentException expected){}
    }
    private static byte[] file(String text) {return text.getBytes(StandardCharsets.UTF_8);}
    public static void main(String[] args) {
        Map<String,String> notes=new TreeMap<>();
        notes.put("2026-10-18","Reunión con mamá\nSegunda línea\t🙂 🇨🇴");
        notes.put("2024-02-29","Cumpleaños bisiesto");
        notes.put("2100-12-31","Última fecha");
        notes.put("2000-01-01","Primera fecha");
        require(NoteBackup.decode(NoteBackup.encode(notes)).equals(notes),"Ida y vuelta UTF-8, emoji, salto y tabulación");
        require(NoteBackup.decode(NoteBackup.encode(Map.of())).isEmpty(),"Archivo vacío válido");
        String crlf=new String(NoteBackup.encode(notes),StandardCharsets.UTF_8).replace("\n","\r\n");
        require(NoteBackup.decode(file(crlf)).equals(notes),"CRLF");
        reject(file("otro formato"),"formato desconocido");
        reject(file("FESTIVOS_COLOMBIA_NOTES\t2\n"),"versión futura");
        reject(file("FESTIVOS_COLOMBIA_NOTES\t1\n2026-02-30\tYQ==\n"),"fecha imposible");
        reject(file("FESTIVOS_COLOMBIA_NOTES\t1\n1999-12-31\tYQ==\n"),"año fuera de rango");
        reject(file("FESTIVOS_COLOMBIA_NOTES\t1\n2026-01-01\t!!!\n"),"base64 roto");
        reject(file("FESTIVOS_COLOMBIA_NOTES\t1\n2026-01-01\tYQ==\n2026-01-01\tYg==\n"),"fecha duplicada");
        reject(file("FESTIVOS_COLOMBIA_NOTES\t1\n2026-01-01\t/w==\n"),"UTF-8 inválido");
        reject(new byte[NoteBackup.MAX_BYTES+1],"archivo demasiado grande");
        try {NoteBackup.encode(Map.of("2026-01-01","x".repeat(4001)));throw new AssertionError("Nota demasiado larga");}catch(IllegalArgumentException expected){}
        require(NoteBackup.decode(NoteBackup.encode(Map.of("2026-01-01","x".repeat(4000)))).get("2026-01-01").length()==4000,"Límite 4000");
        Map<String,String> old=Map.of("2026-01-01","Actual","2026-02-01","Solo local");
        Map<String,String> incoming=Map.of("2026-01-01","Respaldo","2026-03-01","Nueva");
        Map<String,String> preserved=NoteBackup.merge(old,incoming,false);
        require(preserved.get("2026-01-01").equals("Actual")&&preserved.size()==3,"Conservar actuales");
        Map<String,String> replaced=NoteBackup.merge(old,incoming,true);
        require(replaced.get("2026-01-01").equals("Respaldo")&&replaced.get("2026-02-01").equals("Solo local"),"Reemplazar solo coincidencias");
        require(old.size()==2&&old.get("2026-01-01").equals("Actual"),"No mutar originales");
        require(CalendarSearch.find("reunion",2026,notes).size()==1,"Notas sin tildes");
        require(CalendarSearch.find("NAVIDAD",2026,notes).size()==1,"Festivo sin distinguir mayúsculas");
        require(CalendarSearch.find("25/12/2026",2026,notes).get(0).date.equals(LocalDate.of(2026,12,25)),"Fecha dd/MM/aaaa");
        require(CalendarSearch.find("2024-02-29",2026,notes).size()==1,"Nota de otro año por ISO");
        require(CalendarSearch.find("29 febrero 2024",2026,notes).size()==1,"Fecha escrita");
        require(CalendarSearch.find("18/10/2026",2026,notes).size()==1,"Fecha de nota");
        require(CalendarSearch.find("   ",2026,notes).isEmpty(),"Consulta vacía");
        require(CalendarSearch.find("sincoincidencias123",2026,notes).isEmpty(),"Sin resultados");
        require(CalendarSearch.find("Navidad",2027,notes).get(0).date.getYear()==2027,"Año de festivos elegido");
        System.out.println("OK: respaldos, límites, Unicode, rechazos, conflictos y búsquedas.");
    }
}
