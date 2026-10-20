package co.calendario.festivos;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
public final class Holidays {
 public static final class Holiday {
  public final LocalDate date, original; public final String name;
  Holiday(LocalDate date, LocalDate original, String name) { this.date=date; this.original=original; this.name=name; }
 }
 public static LocalDate easter(int y) {
  int a=y%19,b=y/100,c=y%100,d=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3,h=(19*a+b-d-g+15)%30,i=c/4,k=c%4,l=(32+2*e+2*i-h-k)%7,m=(a+11*h+22*l)/451,n=h+l-7*m+114;
  return LocalDate.of(y,n/31,n%31+1);
 }
 private static void add(List<Holiday> list, LocalDate d, String n, boolean move) { list.add(new Holiday(move?d.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)):d,d,n)); }
 public static List<Holiday> year(int y) {
  List<Holiday> r=new ArrayList<>();
  int[][] fixed={{1,1},{5,1},{7,20},{8,7},{12,8},{12,25}};
  String[] fn={"Año Nuevo","Día del Trabajo","Independencia de Colombia","Batalla de Boyacá","Inmaculada Concepción","Navidad"};
  for(int j=0;j<fixed.length;j++) add(r,LocalDate.of(y,fixed[j][0],fixed[j][1]),fn[j],false);
  int[][] moved={{1,6},{3,19},{6,29},{8,15},{10,12},{11,1},{11,11}};
  String[] mn={"Día de los Reyes Magos","Día de San José","San Pedro y San Pablo","Asunción de la Virgen","Diversidad Étnica y Cultural","Todos los Santos","Independencia de Cartagena"};
  for(int j=0;j<moved.length;j++) add(r,LocalDate.of(y,moved[j][0],moved[j][1]),mn[j],true);
  LocalDate e=easter(y);
  add(r,e.minusDays(3),"Jueves Santo",false); add(r,e.minusDays(2),"Viernes Santo",false);
  add(r,e.plusDays(39),"Ascensión del Señor",true); add(r,e.plusDays(60),"Corpus Christi",true); add(r,e.plusDays(68),"Sagrado Corazón de Jesús",true);
  r.sort(Comparator.comparing(h->h.date)); return r;
 }
}
