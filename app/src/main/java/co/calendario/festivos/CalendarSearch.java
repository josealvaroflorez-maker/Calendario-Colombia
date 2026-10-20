package co.calendario.festivos;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class CalendarSearch {
    private static final Locale ES=new Locale("es","CO");
    public static final class Result {
        public final LocalDate date; public final String label;
        Result(LocalDate date,String label) { this.date=date; this.label=label; }
    }
    private static String normalized(String text) {
        return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
    }
    private static boolean matches(LocalDate d,String text,String query) {
        String dates=d+" "+d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy d/M/yyyy dd-MM-yyyy d MMMM yyyy",ES));
        return normalized(text+" "+dates).contains(query);
    }
    public static List<Result> find(String query,int holidayYear,Map<String,String> notes) {
        String q=normalized(query); List<Result> results=new ArrayList<>();
        if(q.isEmpty()) return results;
        DateTimeFormatter f=DateTimeFormatter.ofPattern("dd MMM yyyy",ES);
        for(Holidays.Holiday h:Holidays.year(holidayYear)) if(matches(h.date,h.name,q)) {
            results.add(new Result(h.date,h.date.format(f)+" · Festivo\n"+h.name));
        }
        for(Map.Entry<String,String> e:notes.entrySet()) {
            LocalDate d;
            try { d=LocalDate.parse(e.getKey()); } catch(RuntimeException ignored) { continue; }
            if(d.getYear()<2000||d.getYear()>2100) continue;
            if(matches(d,e.getValue(),q)) {
                String preview=e.getValue().replace('\n',' ');
                if(preview.length()>120) preview=preview.substring(0,120)+"…";
                results.add(new Result(d,d.format(f)+" · Nota\n"+preview));
            }
        }
        results.sort(Comparator.comparing(r->r.date));
        return results;
    }
}
