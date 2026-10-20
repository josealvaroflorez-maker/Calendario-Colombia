package co.calendario.festivos;
import java.time.*;
import java.util.*;
public class HolidaysCheck {
 public static void main(String[] args) {
  String expected="2026-01-01 2026-01-12 2026-03-23 2026-04-02 2026-04-03 2026-05-01 2026-05-18 2026-06-08 2026-06-15 2026-06-29 2026-07-20 2026-08-07 2026-08-17 2026-10-12 2026-11-02 2026-11-16 2026-12-08 2026-12-25";
  StringJoiner actual=new StringJoiner(" "); for(Holidays.Holiday h:Holidays.year(2026)) actual.add(h.date.toString());
  if(!actual.toString().equals(expected)) throw new AssertionError(actual);
  if(!Holidays.easter(2024).equals(LocalDate.of(2024,3,31))) throw new AssertionError("Pascua 2024");
  long shared=Holidays.year(2025).stream().filter(h->h.date.equals(LocalDate.of(2025,6,30))).count();
  if(shared!=2) throw new AssertionError("Coincidencia 2025");
  for(int year=2000;year<=2100;year++) {
   List<Holidays.Holiday> holidays=Holidays.year(year);
   if(holidays.size()!=18) throw new AssertionError("Cantidad "+year);
   for(Holidays.Holiday h:holidays) {
    if(h.date.getYear()!=year) throw new AssertionError("Fuera del año");
    if(!h.date.equals(h.original)&&(h.date.getDayOfWeek()!=DayOfWeek.MONDAY||h.date.isBefore(h.original)||h.date.isAfter(h.original.plusDays(6)))) throw new AssertionError("Traslado incorrecto");
   }
   for(int m=1;m<=12;m++) {
    YearMonth ym=YearMonth.of(year,m); int offset=ym.atDay(1).getDayOfWeek().getValue()-1;
    if(offset+ym.lengthOfMonth()>42) throw new AssertionError("Cuadricula anual incompleta");
   }
  }
  System.out.println("OK: fechas de 2026, Pascua, coincidencias, traslados y cobertura de cuadriculas 2000-2100.");
 }
}
