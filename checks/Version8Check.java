package co.calendario.festivos;
import java.time.YearMonth;
public class Version8Check {
    private static void require(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args) {
        require(CalendarNavigation.move(YearMonth.of(2026,12),false,1).equals(YearMonth.of(2027,1)),"Siguiente mes con cambio de año");
        require(CalendarNavigation.move(YearMonth.of(2024,3),false,-1).equals(YearMonth.of(2024,2)),"Mes bisiesto");
        require(CalendarNavigation.move(YearMonth.of(2026,8),true,-1).equals(YearMonth.of(2025,8)),"Navegación anual");
        require(CalendarNavigation.move(YearMonth.of(2000,1),false,-1).equals(YearMonth.of(2000,1)),"Límite inferior mensual");
        require(CalendarNavigation.move(YearMonth.of(2100,12),false,1).equals(YearMonth.of(2100,12)),"Límite superior mensual");
        require(CalendarNavigation.move(YearMonth.of(2000,6),true,-1).equals(YearMonth.of(2000,6)),"Límite inferior anual");
        require(CalendarNavigation.move(YearMonth.of(2100,6),true,1).equals(YearMonth.of(2100,6)),"Límite superior anual");
        require(CalendarNavigation.swipeDirection(-100,10,300,1)==1,"Deslizar hacia mes siguiente");
        require(CalendarNavigation.swipeDirection(100,10,300,1)==-1,"Deslizar hacia mes anterior");
        require(CalendarNavigation.swipeDirection(10,100,300,1)==0,"Conservar desplazamiento vertical");
        require(CalendarNavigation.swipeDirection(60,0,300,1)==0,"No cambiar por toque corto");
        require(CalendarNavigation.swipeDirection(100,90,300,1)==0,"No cambiar por gesto diagonal");
        require(CalendarNavigation.swipeDirection(100,0,900,1)==0,"No cambiar por arrastre lento");
        require(CalendarNavigation.swipeDirection(100,0,300,2)==0,"Umbral proporcional a densidad");
        require(CalendarNavigation.swipeDirection(-200,0,300,2)==1,"Gesto en pantalla de alta densidad");
        System.out.println("OK: navegación mensual/anual, límites, gestos y desplazamiento vertical.");
    }
}
