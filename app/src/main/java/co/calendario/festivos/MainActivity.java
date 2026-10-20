package co.calendario.festivos;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ColorDrawable;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.Gravity;
import android.graphics.Paint;
import android.text.InputType;
import android.text.InputFilter;
import android.content.SharedPreferences;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
    private int INK, GREEN, RED, HOLIDAY_BG, SURFACE, PAGE;
    private static final int EXPORT_NOTES=701, IMPORT_NOTES=702;
    private final ExecutorService fileWorker=Executors.newSingleThreadExecutor();
    private AlertDialog workingDialog;
    private LocalDate selectedDate;
    private boolean dark;
    private EditText noteEditor;
    private LocalDate editingDate;
    private final Locale locale=new Locale("es", "CO");
    private YearMonth month;
    private LocalDate today;
    private boolean annual;
    private LinearLayout root, body;

    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private String date(LocalDate d) { return d.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",locale)); }
    private TextView text(String s,int size,int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(dp(8),dp(7),dp(8),dp(7)); return t;
    }
    private void bold(TextView t) { t.setTypeface(null,Typeface.BOLD); }
    private GradientDrawable bg(int color) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(14)); return d;
    }
    private Button button(String s,Runnable action) {
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(INK);
        b.setBackground(buttonBackground(SURFACE)); b.setTextSize(15); b.setPadding(dp(10),dp(8),dp(10),dp(8));
        b.setStateListAnimator(null);
        b.setMinHeight(dp(48)); b.setOnClickListener(v->action.run()); return b;
    }
    @Override public void onCreate(Bundle state) {
        dark=getSharedPreferences("appearance",MODE_PRIVATE).getBoolean("dark",false);
        setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(state);
        INK=dark?0xffe6eee9:0xff163c36;
        GREEN=dark?0xff81d4b4:0xff176b56;
        RED=dark?0xffff8e9b:0xffb42332;
        HOLIDAY_BG=dark?0xff47232d:0xffffe8eb;
        SURFACE=dark?0xff1c2c30:0xffffffff;
        PAGE=dark?0xff101d21:0xfff4f5ef;
        getWindow().setStatusBarColor(PAGE);
        getWindow().setNavigationBarColor(PAGE);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        today=LocalDate.now(ZoneId.of("America/Bogota"));
        month=state==null?YearMonth.from(today):YearMonth.parse(state.getString("month"));
        annual=state==null?getPreferences(MODE_PRIVATE).getBoolean("annual",false):state.getBoolean("annual");
        if(state!=null&&state.containsKey("selectedDate")) selectedDate=LocalDate.parse(state.getString("selectedDate"));
        render();
        if(state!=null&&state.containsKey("editingDate")) {
            openNote(LocalDate.parse(state.getString("editingDate")),state.getString("noteDraft",""));
        }
    }
    @Override public void onResume() {
        super.onResume();
        LocalDate now=LocalDate.now(ZoneId.of("America/Bogota"));
        if(!now.equals(today)) { today=now; render(); }
    }
    @Override public void onSaveInstanceState(Bundle state) {
        state.putString("month",month.toString()); state.putBoolean("annual",annual);
        if(editingDate!=null&&noteEditor!=null) {
            state.putString("editingDate",editingDate.toString()); state.putString("noteDraft",noteEditor.getText().toString());
        }
        if(selectedDate!=null) state.putString("selectedDate",selectedDate.toString());
        super.onSaveInstanceState(state);
    }
    @Override public void onDestroy() {
        if(workingDialog!=null) workingDialog.dismiss();
        fileWorker.shutdown(); super.onDestroy();
    }
    private void setAnnual(boolean value) {
        annual=value; getPreferences(MODE_PRIVATE).edit().putBoolean("annual",value).apply(); render();
    }
    private void step(int direction) {
        YearMonth next=CalendarNavigation.move(month,annual,direction);
        if(!next.equals(month)) { month=next; render(); }
    }
    private void render() {
        root=new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(PAGE);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            root.setPadding(dp(12),insets.getSystemWindowInsetTop(),dp(12),insets.getSystemWindowInsetBottom());
            return insets;
        }); root.requestApplyInsets();
        LinearLayout heading=new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text(getString(co.calendario.festivos.R.string.app_name),23,INK); bold(title);
        heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button menu=button("☰",()->showTools()); menu.setTextSize(23); menu.setContentDescription("Abrir menú de herramientas");
        heading.addView(menu,new LinearLayout.LayoutParams(dp(52),dp(52))); root.addView(heading);
        LinearLayout modes=new LinearLayout(this);
        for(boolean value:new boolean[]{false,true}) {
            Button b=button(value?"Anual":"Mensual",()->setAnnual(value));
            b.setSelected(annual==value);
            b.setContentDescription((value?"Vista anual":"Vista mensual")+(annual==value?", seleccionada":""));
            b.setBackground(buttonBackground(annual==value?GREEN:SURFACE));
            b.setTextColor(annual==value?(dark?PAGE:Color.WHITE):INK);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);
            lp.setMargins(dp(3),dp(3),dp(3),dp(5)); modes.addView(b,lp);
        } root.addView(modes);
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=button("‹",()->step(-1)); prev.setContentDescription(annual?"Año anterior":"Mes anterior");
        prev.setEnabled(annual?month.getYear()>2000:month.isAfter(YearMonth.of(2000,1)));
        prev.setAlpha(prev.isEnabled()?1f:0.35f); nav.addView(prev,new LinearLayout.LayoutParams(dp(52),dp(48)));
        TextView label=text(annual?String.valueOf(month.getYear()):month.format(DateTimeFormatter.ofPattern("MMMM yyyy",locale)),20,INK);
        bold(label); label.setGravity(Gravity.CENTER); label.setMinHeight(dp(48));
        label.setContentDescription(label.getText()+". Toca para seleccionar año"); label.setOnClickListener(v->chooseYear());
        nav.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button next=button("›",()->step(1)); next.setContentDescription(annual?"Año siguiente":"Mes siguiente");
        next.setEnabled(annual?month.getYear()<2100:month.isBefore(YearMonth.of(2100,12)));
        next.setAlpha(next.isEnabled()?1f:0.35f); nav.addView(next,new LinearLayout.LayoutParams(dp(52),dp(48))); root.addView(nav);
        ScrollView scroll=new CalendarScroll(); body=new LinearLayout(this); body.setOrientation(1);
        scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(button("Hoy · "+date(today),()->{month=YearMonth.from(today);render();}));
        body.addView(text(annual?"Desliza para cambiar de año · Toca un mes para abrirlo":"Desliza para cambiar de mes · Toca una fecha para ver sus notas",12,GREEN));
        List<Holidays.Holiday> holidays=Holidays.year(month.getYear());
        if(annual) renderYear(holidays); else {
            body.addView(calendar(month,holidays,false)); renderHolidayList(holidays,false);
        }
        TextView legend=text("Domingos y festivos en rojo · Hoy con borde verde\nFechas subrayadas: tienen una nota · Borde dorado: fecha elegida",12,RED); body.addView(legend);
        if(!annual) body.addView(button("Ver calendario anual",()->setAnnual(true)));
        body.addView(text("Toca el encabezado para elegir año · 2000–2100\nTu vista elegida se guarda automáticamente.\nFestivos nacionales según Ley 51 de 1983. No incluye celebraciones locales ni futuros cambios normativos.\nCalendario Colombia · Versión 8.0 · Sin conexión y sin permisos",12,GREEN));
    }
    private LinearLayout calendar(YearMonth ym,List<Holidays.Holiday> holidays,boolean compact) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(1);
        LinearLayout week=new LinearLayout(this);
        for(String s:new String[]{"L","M","M","J","V","S","D"}) {
            TextView t=text(s,compact?11:13,s.equals("D")?RED:GREEN); t.setPadding(0,dp(4),0,dp(4)); t.setGravity(Gravity.CENTER);
            week.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        } box.addView(week);
        int offset=ym.atDay(1).getDayOfWeek().getValue()-1;
        int rows=compact?6:(offset+ym.lengthOfMonth()+6)/7;
        for(int row=0;row<rows;row++) {
            LinearLayout line=new LinearLayout(this);
            for(int col=0;col<7;col++) {
                int day=row*7+col-offset+1;
                TextView cell=text("",compact?13:18,INK); cell.setPadding(0,0,0,0); cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(compact?28:48),1);
                cp.setMargins(dp(1),dp(2),dp(1),dp(2)); line.addView(cell,cp);
                if(day<1||day>ym.lengthOfMonth()) continue;
                LocalDate d=ym.atDay(day); List<Holidays.Holiday> matches=new ArrayList<>();
                for(Holidays.Holiday h:holidays) if(h.date.equals(d)) matches.add(h);
                boolean holiday=!matches.isEmpty();
                boolean sunday=d.getDayOfWeek()==DayOfWeek.SUNDAY;
                cell.setText(String.valueOf(day));
                if(sunday) cell.setTextColor(RED);
                GradientDrawable shape=bg(holiday?HOLIDAY_BG:Color.TRANSPARENT);
                if(holiday) { cell.setTextColor(RED); bold(cell); }
                if(d.equals(today)) { shape.setStroke(dp(2),GREEN); if(!holiday&&!sunday) cell.setTextColor(GREEN); bold(cell); }
                if(d.equals(selectedDate)&&!d.equals(today)) shape.setStroke(dp(2),dark?0xffffcf66:0xff946000);
                cell.setBackground(shape);
                boolean hasNote=!notes().getString(d.toString(),"").isEmpty();
                if(hasNote) cell.setPaintFlags(cell.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
                StringBuilder detail=new StringBuilder();
                for(Holidays.Holiday h:matches) {
                    detail.append(h.name);
                    if(!h.date.equals(h.original)) detail.append("\nFecha original: ").append(date(h.original)).append(". Descanso trasladado al lunes.");
                    detail.append("\n\n");
                }
                cell.setContentDescription(date(d)+(holiday?", festivo: "+detail:"")+(d.equals(today)?", hoy":"")+(hasNote?", tiene una nota":"")+(d.equals(selectedDate)?", fecha elegida":""));
                // Miniaturas sin objetivos táctiles pequeños: su botón abre el mes completo.
                if(!compact) cell.setOnClickListener(v->openNote(d,null));
            } box.addView(line);
        } return box;
    }
    private void renderYear(List<Holidays.Holiday> holidays) {
        int columns=getResources().getConfiguration().screenWidthDp>=360
            &&getResources().getConfiguration().fontScale<=1.3f?2:1;
        for(int start=1;start<=12;start+=columns) {
            LinearLayout row=new LinearLayout(this);
            for(int c=0;c<columns&&start+c<=12;c++) {
                YearMonth ym=YearMonth.of(month.getYear(),start+c);
                LinearLayout card=new LinearLayout(this); card.setOrientation(1); card.setPadding(dp(4),dp(4),dp(4),dp(4));
                card.setBackground(bg(SURFACE));
                Button heading=button(ym.format(DateTimeFormatter.ofPattern("MMMM",locale)),()->{month=ym;setAnnual(false);});
                heading.setContentDescription("Abrir "+ym.format(DateTimeFormatter.ofPattern("MMMM yyyy",locale))+" en vista mensual");
                card.addView(heading); card.addView(calendar(ym,holidays,true));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),dp(5),dp(3),dp(5));
                row.addView(card,lp);
            } body.addView(row);
        }
        renderHolidayList(holidays,true);
    }
    private void renderHolidayList(List<Holidays.Holiday> holidays,boolean year) {
        TextView heading=text(year?"Festivos de "+month.getYear():"Festivos del mes",21,INK); bold(heading); body.addView(heading);
        int count=0;
        for(Holidays.Holiday h:holidays) if(year||YearMonth.from(h.date).equals(month)) {
            count++;
            TextView t=text(h.date.format(DateTimeFormatter.ofPattern("dd MMM",locale))+" · "+h.name+"\n"+date(h.date)
                +(h.date.equals(h.original)?"":" · Trasladado"),15,RED);
            t.setBackground(bg(SURFACE));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(4),0,dp(4)); body.addView(t,lp);
        }
        if(count==0) body.addView(text("Un mes sin festivos nacionales.",15,GREEN));
    }
    private SharedPreferences notes() { return getSharedPreferences("date_notes",MODE_PRIVATE); }
    private void openNote(LocalDate day,String draft) {
        String saved=notes().getString(day.toString(),"");
        LinearLayout content=new LinearLayout(this); content.setOrientation(1); content.setPadding(dp(16),0,dp(16),0);
        StringBuilder detail=new StringBuilder();
        for(Holidays.Holiday h:Holidays.year(day.getYear())) if(h.date.equals(day)) {
            detail.append(h.name);
            if(!h.date.equals(h.original)) detail.append("\nFecha original: ").append(date(h.original)).append(". Descanso trasladado al lunes.");
            detail.append("\n\n");
        }
        content.addView(text(detail.length()>0?detail.toString().trim():"No es festivo nacional.",16,detail.length()>0?RED:INK));
        EditText input=new EditText(this); input.setHint("Escribe tu nota para este día…");
        input.setTextColor(INK); input.setHintTextColor(GREEN); input.setMinLines(4); input.setMaxLines(8);
        input.setTextSize(17); input.setPadding(dp(8),dp(12),dp(8),dp(12)); input.setLineSpacing(dp(3),1f);
        input.setGravity(Gravity.TOP); input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4000)}); input.setText(draft==null?saved:draft);
        content.addView(input,new LinearLayout.LayoutParams(-1,-2));
        TextView noteCount=text("0 / 4000 caracteres · Solo en este teléfono",12,GREEN);
        content.addView(noteCount);
        android.text.TextWatcher watcher=new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence v,int start,int count,int after) { }
            public void onTextChanged(CharSequence v,int start,int before,int count) {noteCount.setText(v.length()+" / 4000 caracteres · Solo en este teléfono");}
            public void afterTextChanged(android.text.Editable value) { }
        };
        input.addTextChangedListener(watcher); noteCount.setText(input.length()+" / 4000 caracteres · Solo en este teléfono");
        ScrollView scroll=new ScrollView(this); scroll.addView(content);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(date(day)+" de "+day.getYear()).setView(scroll)
            .setPositiveButton("Guardar",(dialog,which)->{
                String value=input.getText().toString().trim();
                if(value.isEmpty()) notes().edit().remove(day.toString()).apply();
                else notes().edit().putString(day.toString(),value).apply();
                render(); Toast.makeText(this,value.isEmpty()?"Sin nota guardada":"Nota guardada",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancelar",null);
        if(!saved.isEmpty()) builder.setNeutralButton("Eliminar",(dialog,which)->new AlertDialog.Builder(this)
            .setTitle("¿Eliminar la nota?").setMessage(date(day))
            .setPositiveButton("Eliminar",(d,w)->{notes().edit().remove(day.toString()).apply();render();})
            .setNegativeButton("Cancelar",null).show());
        AlertDialog dialog=builder.create(); editingDate=day; noteEditor=input;
        dialog.setOnDismissListener(d->{if(noteEditor==input){editingDate=null;noteEditor=null;}});
        dialog.show();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void showNotes() {
        List<LocalDate> days=new ArrayList<>();
        for(String key:notes().getAll().keySet()) {
            try { LocalDate day=LocalDate.parse(key); if(!notes().getString(key,"").isEmpty()) days.add(day); }
            catch(java.time.format.DateTimeParseException ignored) { }
        }
        Collections.sort(days);
        if(days.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Mis notas").setMessage("Todavía no tienes notas. En la vista mensual, toca cualquier fecha para escribir una.")
                .setPositiveButton("Cerrar",null).show(); return;
        }
        List<String> labels=new ArrayList<>();
        for(int i=0;i<days.size();i++) {
            String note=notes().getString(days.get(i).toString(),"").replace('\n',' ');
            labels.add(days.get(i).format(DateTimeFormatter.ofPattern("EEEE dd MMM yyyy",locale))+"\n"+(note.length()>180?note.substring(0,180)+"…":note));
        }
        new AlertDialog.Builder(this).setTitle("Mis notas · "+days.size()).setAdapter(resultAdapter(labels),(dialog,index)->{
            month=YearMonth.from(days.get(index)); annual=false; getPreferences(MODE_PRIVATE).edit().putBoolean("annual",false).apply();
            render(); openNote(days.get(index),null);
        }).setNegativeButton("Cerrar",null).show();
    }
    private RippleDrawable buttonBackground(int color) {
        return new RippleDrawable(ColorStateList.valueOf(dark?0x30ffffff:0x30176b56),bg(color),null);
    }
    private void showTools() {
        String[] labels={"Buscar festivos y notas","Ir a una fecha","Mis notas","Respaldo de notas",dark?"Activar modo claro":"Activar modo oscuro","Acerca de Calendario Colombia"};
        new AlertDialog.Builder(this).setTitle("Herramientas").setItems(labels,(dialog,index)->{
            switch(index) {
                case 0:showSearch();break;
                case 1:chooseDate();break;
                case 2:showNotes();break;
                case 3:backupMenu();break;
                case 4:getSharedPreferences("appearance",MODE_PRIVATE).edit().putBoolean("dark",!dark).apply();recreate();break;
                case 5:message("Calendario Colombia · 8.0","Calendario de festivos nacionales y notas personales.\n\nUsa las flechas o desliza para cambiar de mes o año. Las herramientas están en el menú superior.\n\nSin conexión. Tus notas se guardan en el teléfono; puedes respaldarlas desde Herramientas.");break;
                default:break;
            }
        }).setNegativeButton("Cerrar",null).show();
    }
    private ArrayAdapter<String> resultAdapter(List<String> labels) {
        return new ArrayAdapter<String>(this,android.R.layout.simple_list_item_2,android.R.id.text1,labels) {
            @Override public android.view.View getView(int position,android.view.View convert,android.view.ViewGroup parent) {
                android.view.View row=super.getView(position,convert,parent);
                TextView dateText=row.findViewById(android.R.id.text1), preview=row.findViewById(android.R.id.text2);
                String value=getItem(position); if(value==null)value="";
                int split=value.indexOf('\n');
                dateText.setText(split>=0?value.substring(0,split):value);
                preview.setText(split>=0?value.substring(split+1):"");
                dateText.setTextSize(16); dateText.setTextColor(GREEN); dateText.setTypeface(null,Typeface.BOLD);
                dateText.setSingleLine(false); dateText.setMaxLines(2);
                preview.setTextSize(16); preview.setTextColor(INK); preview.setSingleLine(false);
                preview.setMaxLines(3); preview.setEllipsize(TextUtils.TruncateAt.END); preview.setLineSpacing(dp(2),1f);
                row.setPadding(dp(12),dp(10),dp(12),dp(10)); row.setMinimumHeight(dp(76));
                row.setBackgroundColor(SURFACE); return row;
            }
        };
    }
    private final class CalendarScroll extends ScrollView {
        private float startX,startY; private long startTime;
        private boolean captured,vertical,multiTouch;
        CalendarScroll() {super(MainActivity.this);}
        @Override public boolean dispatchTouchEvent(MotionEvent event) {
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN) {
                startX=event.getX();startY=event.getY();startTime=event.getEventTime();captured=false;vertical=false;multiTouch=false;
            } else if(event.getPointerCount()>1||action==MotionEvent.ACTION_POINTER_DOWN) multiTouch=true;
            float dx=event.getX()-startX,dy=event.getY()-startY;
            if(action==MotionEvent.ACTION_MOVE&&!captured&&!multiTouch) {
                if(Math.abs(dy)>dp(12)&&Math.abs(dy)>Math.abs(dx))vertical=true;
                if(!vertical&&CalendarNavigation.swipeDirection(dx,dy,event.getEventTime()-startTime,getResources().getDisplayMetrics().density)!=0) {
                    MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();captured=true;
                }
            }
            if(captured) {
                if(action==MotionEvent.ACTION_UP) {
                    int direction=multiTouch?0:CalendarNavigation.swipeDirection(dx,dy,event.getEventTime()-startTime,getResources().getDisplayMetrics().density);
                    captured=false;if(direction!=0)step(direction);
                } else if(action==MotionEvent.ACTION_CANCEL)captured=false;
                return true;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private Map<String,String> snapshotNotes() {
        Map<String,String> result=new TreeMap<>();
        for(Map.Entry<String,?> entry:notes().getAll().entrySet())
            if(entry.getValue() instanceof String&&!((String)entry.getValue()).trim().isEmpty()) result.put(entry.getKey(),(String)entry.getValue());
        return result;
    }
    private void message(String title,String value) {
        if(!isFinishing()&&!isDestroyed()) new AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("Aceptar",null).show();
    }
    private void goToDate(LocalDate day) {
        selectedDate=day; month=YearMonth.from(day); setAnnual(false); openNote(day,null);
    }
    private void chooseDate() {
        LocalDate start=selectedDate!=null&&YearMonth.from(selectedDate).equals(month)?selectedDate:
            YearMonth.from(today).equals(month)?today:month.atDay(1);
        DatePickerDialog dialog=new DatePickerDialog(this,(picker,y,m,d)->goToDate(LocalDate.of(y,m+1,d)),start.getYear(),start.getMonthValue()-1,start.getDayOfMonth());
        dialog.getDatePicker().setMinDate(LocalDate.of(2000,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.getDatePicker().setMaxDate(LocalDate.of(2100,12,31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.setTitle("Ir a una fecha"); dialog.show();
    }
    private void showSearch() {
        final int year=month.getYear();
        Map<String,String> savedNotes=snapshotNotes();
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(1); panel.setPadding(dp(16),0,dp(16),0);
        panel.addView(text("Festivos de "+year+" y todas tus notas. Busca un nombre, palabra o fecha (dd/MM/aaaa).",13,GREEN));
        EditText query=new EditText(this); query.setSingleLine(true); query.setHint("Nombre, palabra o fecha"); query.setTextSize(17);
        query.setTextColor(INK); query.setHintTextColor(GREEN); query.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH); panel.addView(query);
        TextView status=text("Escribe y pulsa Buscar.",13,GREEN); panel.addView(status);
        List<CalendarSearch.Result> matches=new ArrayList<>(); List<String> labels=new ArrayList<>();
        ArrayAdapter<String> adapter=resultAdapter(labels);
        ListView list=new ListView(this); list.setAdapter(adapter); list.setDivider(new ColorDrawable(PAGE)); list.setDividerHeight(dp(6));
        Button search=button("Buscar",()->{}); panel.addView(search);
        panel.addView(list,new LinearLayout.LayoutParams(-1,dp(Math.max(140,Math.min(300,getResources().getConfiguration().screenHeightDp-380)))));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Buscar").setView(panel).setNegativeButton("Cerrar",null).create();
        int[] generation={0};
        Runnable runSearch=()->{
            String value=query.getText().toString(); int request=++generation[0];
            if(value.trim().isEmpty()) { matches.clear(); labels.clear(); adapter.notifyDataSetChanged(); status.setText("Escribe una palabra o una fecha."); return; }
            matches.clear(); labels.clear(); adapter.notifyDataSetChanged();
            status.setText("Buscando…");
            android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(keyboard!=null) keyboard.hideSoftInputFromWindow(query.getWindowToken(),0);
            fileWorker.execute(()->{
                List<CalendarSearch.Result> found=CalendarSearch.find(value,year,savedNotes);
                runOnUiThread(()->{
                    if(isDestroyed()||!dialog.isShowing()||request!=generation[0]) return;
                    matches.clear(); labels.clear();
                    for(int i=0;i<Math.min(200,found.size());i++) { matches.add(found.get(i)); labels.add(found.get(i).label); }
                    adapter.notifyDataSetChanged();
                    status.setText(found.isEmpty()?"Sin coincidencias.":found.size()>200?"Mostrando 200 de "+found.size()+" resultados. Afina la búsqueda.":found.size()+" resultados. Toca uno para abrir la fecha.");
                });
            });
        };
        search.setOnClickListener(v->runSearch.run());
        query.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){runSearch.run();return true;}return false;});
        list.setOnItemClickListener((parent,v,index,id)->{LocalDate day=matches.get(index).date;dialog.dismiss();goToDate(day);});
        dialog.show(); dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void backupMenu() {
        new AlertDialog.Builder(this).setTitle("Respaldo de notas").setItems(new String[]{"Exportar mis notas","Importar un respaldo"},(dialog,which)->{
            try {
                if(which==0) {
                    if(snapshotNotes().isEmpty()){message("Sin notas","Todavía no tienes notas para respaldar.");return;}
                    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream");
                    intent.putExtra(Intent.EXTRA_TITLE,"Calendario-Colombia-notas-"+today+".fco"); startActivityForResult(intent,EXPORT_NOTES);
                } else {
                    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                    startActivityForResult(intent,IMPORT_NOTES);
                }
            } catch(ActivityNotFoundException ex) { message("No se pudo abrir Archivos","Tu teléfono no tiene un selector de documentos disponible."); }
        }).setNegativeButton("Cerrar",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if((request!=EXPORT_NOTES&&request!=IMPORT_NOTES)||result!=RESULT_OK||data==null||data.getData()==null) return;
        Uri uri=data.getData();
        if(request==EXPORT_NOTES) exportNotes(uri); else importNotes(uri);
    }
    private void startWorking(String text) {
        workingDialog=new AlertDialog.Builder(this).setMessage(text).setCancelable(false).create(); workingDialog.show();
    }
    private void stopWorking() { if(workingDialog!=null){workingDialog.dismiss();workingDialog=null;} }
    private void exportNotes(Uri uri) {
        Map<String,String> saved=snapshotNotes(); startWorking("Guardando respaldo…");
        fileWorker.execute(()->{
            String error=null;
            try {
                byte[] bytes=NoteBackup.encode(saved);
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")) {
                    if(out==null) throw new IOException("No se pudo abrir el archivo."); out.write(bytes); out.flush();
                }
            } catch(Exception ex) { error="No se pudo guardar el respaldo. Comprueba el espacio y el acceso al archivo e inténtalo otra vez."; }
            String failure=error;
            runOnUiThread(()->{if(isDestroyed())return;stopWorking();message(failure==null?"Respaldo guardado":"Error al exportar",failure==null?saved.size()+" notas guardadas en el archivo seleccionado.":failure);});
        });
    }
    private void importNotes(Uri uri) {
        startWorking("Leyendo respaldo…");
        fileWorker.execute(()->{
            Map<String,String> incoming=null; String error=null;
            try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                if(in==null) throw new IOException("No se pudo abrir el archivo.");
                byte[] buffer=new byte[8192]; int n;
                while((n=in.read(buffer))!=-1) { if(out.size()+n>NoteBackup.MAX_BYTES) throw new IllegalArgumentException("El archivo supera 16 MB.");out.write(buffer,0,n); }
                incoming=NoteBackup.decode(out.toByteArray());
            } catch(IllegalArgumentException ex) { error="El archivo no es un respaldo válido de Calendario Colombia o está dañado. No se modificaron tus notas."; }
            catch(Exception ex) { error="No se pudo leer el archivo. Comprueba el acceso e inténtalo otra vez."; }
            Map<String,String> loaded=incoming; String failure=error;
            runOnUiThread(()->{if(isDestroyed())return;stopWorking();if(failure!=null)message("No se pudo importar",failure);else confirmImport(loaded);});
        });
    }
    private void confirmImport(Map<String,String> incoming) {
        if(incoming.isEmpty()){message("Respaldo vacío","El archivo no contiene notas. No se modificó ninguna fecha.");return;}
        Map<String,String> existing=snapshotNotes(); int fresh=0, conflicts=0, identical=0;
        for(Map.Entry<String,String> entry:incoming.entrySet()) {
            if(!existing.containsKey(entry.getKey()))fresh++;
            else if(existing.get(entry.getKey()).equals(entry.getValue()))identical++;
            else conflicts++;
        }
        if(fresh==0&&conflicts==0){message("Notas ya guardadas","Todas las notas de este respaldo ya están en el teléfono.");return;}
        String summary=incoming.size()+" notas en el respaldo.\n"+fresh+" nuevas · "+conflicts+" con texto diferente · "+identical+" iguales.\n\nLas notas de otras fechas se conservan.";
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle("Importar notas").setMessage(summary)
            .setNegativeButton("Cancelar",null);
        if(conflicts==0) builder.setPositiveButton("Importar",(d,w)->applyImport(incoming,false));
        else {
            builder.setPositiveButton("Conservar actuales",(d,w)->applyImport(incoming,false));
            builder.setNeutralButton("Usar respaldo",(d,w)->new AlertDialog.Builder(this).setTitle("¿Reemplazar coincidencias?")
                .setMessage("Se reemplazará el texto de las fechas con notas distintas usando el del respaldo. Las demás notas se conservan.")
                .setPositiveButton("Reemplazar",(a,b)->applyImport(incoming,true)).setNegativeButton("Cancelar",null).show());
        }
        builder.show();
    }
    private void applyImport(Map<String,String> incoming,boolean replaceConflicts) {
        Map<String,String> existing=snapshotNotes();
        Map<String,String> merged=NoteBackup.merge(existing,incoming,replaceConflicts);
        startWorking("Importando notas…");
        fileWorker.execute(()->{
            SharedPreferences.Editor editor=notes().edit();
            int changed=0;
            for(Map.Entry<String,String> entry:merged.entrySet()) if(!Objects.equals(existing.get(entry.getKey()),entry.getValue())) {editor.putString(entry.getKey(),entry.getValue());changed++;}
            boolean success=editor.commit(); int count=changed;
            runOnUiThread(()->{if(isDestroyed())return;stopWorking();render();message(success?"Importación terminada":"No se pudo confirmar el guardado",success?count+" notas añadidas o actualizadas.":"El teléfono no confirmó el guardado. Conserva el respaldo e inténtalo otra vez.");});
        });
    }

    private void chooseYear() {
        NumberPicker picker=new NumberPicker(this); picker.setMinValue(2000); picker.setMaxValue(2100); picker.setValue(month.getYear());
        new AlertDialog.Builder(this).setTitle("Seleccionar año").setView(picker)
            .setPositiveButton("Ver",(d,w)->{month=YearMonth.of(picker.getValue(),month.getMonthValue());render();})
            .setNegativeButton("Cancelar",null).show();
    }
}
