package com.godnit.ourempire

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.Bundle
import android.view.*
import kotlin.math.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContentView(GameView(this))
    }
}

private enum class S { HOME, SETUP, MAP, COUNTRY, DIP, ARMY, ORG, SETTINGS }
private data class R(val id:String,val ar:String,val en:String,val p:FloatArray,val owner:String,val cityAr:String,val cityEn:String){
    fun c()=PointF(p.filterIndexed{i,_->i%2==0}.average().toFloat(),p.filterIndexed{i,_->i%2==1}.average().toFloat())
    fun hit(x:Float,y:Float):Boolean{var z=false;var j=p.size-2;var i=0;while(i<p.size){val xi=p[i];val yi=p[i+1];val xj=p[j];val yj=p[j+1];if((yi>y)!=(yj>y)&&x<(xj-xi)*(y-yi)/((yj-yi).let{if(abs(it)<.001f).001f else it})+xi)z=!z;j=i;i+=2};return z}
}

private class GameView(ctx:Context):View(ctx){
    private val d=resources.displayMetrics.density
    private val q=Paint(Paint.ANTI_ALIAS_FLAG)
    private val o=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE}
    private val hits=hashMapOf<String,RectF>()
    private var s=S.HOME; private var ar=true; private var started=false
    private var year=1914; private var mode=0; private var diff=1; private var me="YEM"; private var target="FRA"
    private var turn=1; private var gold=4500; private var food=780; private var recruits=900; private var armies=3; private var stability=86
    private var zoom=1f; private var panX=0f; private var panY=0f; private var selected:R?=null

    private val ca=mapOf("YEM" to "اليمن","GBR" to "بريطانيا","FRA" to "فرنسا","ESP" to "إسبانيا","PRT" to "البرتغال","DEU" to "ألمانيا","ITA" to "إيطاليا","RUS" to "روسيا","SWE" to "السويد","POL" to "بولندا","TUR" to "تركيا","EGY" to "مصر","SAU" to "الجزيرة العربية","IRQ" to "العراق","LEV" to "بلاد الشام","GRC" to "اليونان","BALK" to "البلقان","UKR" to "أوكرانيا","MAR" to "المغرب","DZA" to "الجزائر","LBY" to "ليبيا","TUN" to "تونس","OTT" to "الدولة العثمانية","COL_GBR" to "الإمبراطورية البريطانية","COL_FRA" to "الاستعمار الفرنسي","COL_ITA" to "إيطاليا","GER41" to "ألمانيا","NAP" to "الإمبراطورية الفرنسية")
    private val ce=mapOf("YEM" to "Yemen","GBR" to "Britain","FRA" to "France","ESP" to "Spain","PRT" to "Portugal","DEU" to "Germany","ITA" to "Italy","RUS" to "Russia","SWE" to "Sweden","POL" to "Poland","TUR" to "Turkey","EGY" to "Egypt","SAU" to "Arabia","IRQ" to "Iraq","LEV" to "Levant","GRC" to "Greece","BALK" to "Balkans","UKR" to "Ukraine","MAR" to "Morocco","DZA" to "Algeria","LBY" to "Libya","TUN" to "Tunisia","OTT" to "Ottoman Empire","COL_GBR" to "British Empire","COL_FRA" to "French Colonial Rule","COL_ITA" to "Italy","GER41" to "Germany","NAP" to "French Empire")
    private val col=mapOf("YEM" to Color.rgb(126,69,54),"GBR" to Color.rgb(126,113,85),"FRA" to Color.rgb(82,108,128),"ESP" to Color.rgb(159,124,79),"PRT" to Color.rgb(104,129,92),"DEU" to Color.rgb(97,104,94),"ITA" to Color.rgb(111,130,94),"RUS" to Color.rgb(104,128,92),"SWE" to Color.rgb(112,134,135),"POL" to Color.rgb(129,111,117),"TUR" to Color.rgb(145,92,77),"EGY" to Color.rgb(157,134,92),"SAU" to Color.rgb(98,129,100),"IRQ" to Color.rgb(123,108,84),"LEV" to Color.rgb(126,118,92),"GRC" to Color.rgb(90,117,132),"BALK" to Color.rgb(113,106,124),"UKR" to Color.rgb(135,133,92),"MAR" to Color.rgb(116,138,96),"DZA" to Color.rgb(111,129,88),"LBY" to Color.rgb(146,117,91),"TUN" to Color.rgb(132,110,92),"OTT" to Color.rgb(120,86,67),"COL_GBR" to Color.rgb(104,101,83),"COL_FRA" to Color.rgb(77,104,125),"COL_ITA" to Color.rgb(94,120,85),"GER41" to Color.rgb(85,92,84),"NAP" to Color.rgb(87,105,122))
    private fun f(vararg a:Float)=floatArrayOf(*a)
    private val rs=listOf(
        R("portugal","البرتغال","Portugal",f(130f,315f,160f,295f,175f,335f,160f,405f,132f,398f),"PRT","لشبونة","Lisbon"),
        R("spain","إسبانيا","Spain",f(165f,286f,275f,276f,320f,335f,290f,405f,180f,410f,160f,360f),"ESP","مدريد","Madrid"),
        R("france","فرنسا","France",f(280f,235f,390f,220f,430f,280f,395f,350f,315f,340f,275f,290f),"FRA","باريس","Paris"),
        R("britain","بريطانيا","Britain",f(250f,100f,286f,82f,310f,145f,290f,215f,255f,190f,240f,135f),"GBR","لندن","London"),
        R("germany","ألمانيا","Germany",f(410f,205f,500f,205f,525f,285f,475f,330f,420f,290f),"DEU","برلين","Berlin"),
        R("poland","بولندا","Poland",f(510f,195f,600f,200f,620f,270f,565f,305f,520f,280f),"POL","وارسو","Warsaw"),
        R("italy","إيطاليا","Italy",f(415f,320f,455f,310f,488f,360f,470f,410f,505f,460f,475f,485f,440f,420f),"ITA","روما","Rome"),
        R("balkans","البلقان","Balkans",f(500f,310f,610f,300f,650f,370f,610f,420f,535f,405f),"BALK","بلغراد","Belgrade"),
        R("greece","اليونان","Greece",f(560f,410f,610f,405f,625f,460f,580f,485f,550f,455f),"GRC","أثينا","Athens"),
        R("scandinavia","إسكندنافيا","Scandinavia",f(430f,60f,490f,40f,535f,80f,520f,175f,475f,195f,445f,150f),"SWE","ستوكهولم","Stockholm"),
        R("ukraine","أوكرانيا","Ukraine",f(610f,210f,725f,205f,760f,270f,700f,315f,615f,285f),"UKR","كييف","Kyiv"),
        R("russia","روسيا","Russia",f(535f,80f,1080f,75f,1135f,245f,1040f,350f,760f,300f,725f,200f,600f,190f),"RUS","موسكو","Moscow"),
        R("turkey","الأناضول","Anatolia",f(625f,390f,785f,375f,845f,415f,805f,455f,650f,455f,610f,425f),"TUR","إسطنبول","Istanbul"),
        R("levant","بلاد الشام","Levant",f(790f,455f,835f,455f,850f,535f,805f,555f,780f,505f),"LEV","دمشق","Damascus"),
        R("iraq","العراق","Iraq",f(835f,455f,905f,450f,940f,515f,900f,565f,850f,535f),"IRQ","بغداد","Baghdad"),
        R("arabia","الجزيرة العربية","Arabia",f(805f,550f,930f,555f,970f,650f,890f,695f,790f,645f),"SAU","الرياض","Riyadh"),
        R("yemen","اليمن","Yemen",f(790f,645f,890f,695f,845f,730f,760f,705f),"YEM","صنعاء","Sana'a"),
        R("egypt","مصر","Egypt",f(650f,505f,765f,505f,790f,620f,690f,630f,640f,565f),"EGY","القاهرة","Cairo"),
        R("libya","ليبيا","Libya",f(520f,500f,650f,500f,640f,625f,535f,635f),"LBY","طرابلس","Tripoli"),
        R("tunisia","تونس","Tunisia",f(465f,470f,515f,470f,525f,535f,485f,555f,455f,520f),"TUN","تونس","Tunis"),
        R("algeria","الجزائر","Algeria",f(300f,445f,460f,440f,485f,555f,445f,650f,300f,630f,255f,520f),"DZA","الجزائر","Algiers"),
        R("morocco","المغرب","Morocco",f(170f,430f,300f,440f,255f,520f,155f,535f,125f,485f),"MAR","الرباط","Rabat")
    )

    private val scale=ScaleGestureDetector(ctx,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(g:ScaleGestureDetector):Boolean{if(s!=S.MAP)return false;zoom=(zoom*g.scaleFactor).coerceIn(.75f,5.2f);invalidate();return true}})
    private val gest=GestureDetector(ctx,object:GestureDetector.SimpleOnGestureListener(){override fun onDown(e:MotionEvent)=true;override fun onScroll(a:MotionEvent?,b:MotionEvent,dx:Float,dy:Float):Boolean{if(s!=S.MAP||scale.isInProgress)return false;panX=(panX-dx).coerceIn(-width*1.8f,width*1.8f);panY=(panY-dy).coerceIn(-height*1.5f,height*1.5f);invalidate();return true};override fun onSingleTapUp(e:MotionEvent):Boolean{tap(e.x,e.y);return true};override fun onDoubleTap(e:MotionEvent):Boolean{if(s==S.MAP){zoom=if(zoom<2f)2.5f else 1f;panX=0f;panY=0f;invalidate()};return true}})
    override fun onTouchEvent(e:MotionEvent):Boolean{scale.onTouchEvent(e);gest.onTouchEvent(e);return true}
    override fun onDraw(c:Canvas){hits.clear();c.drawColor(Color.rgb(18,23,24));when(s){S.HOME->home(c);S.SETUP->setup(c);S.MAP->map(c);S.COUNTRY->country(c);S.DIP->dip(c);S.ARMY->army(c);S.ORG->org(c);S.SETTINGS->settings(c)}}

    private fun t(a:String,e:String)=if(ar)a else e
    private fun n(k:String)=if(ar)ca[k]?:k else ce[k]?:k
    private fun text(c:Canvas,x:String,px:Float,py:Float,z:Float,color:Int=Color.WHITE,align:Paint.Align=Paint.Align.LEFT,b:Boolean=false){q.shader=null;q.style=Paint.Style.FILL;q.color=color;q.textSize=z*d;q.textAlign=align;q.typeface=if(b)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;c.drawText(x,px,py,q)}
    private fun box(c:Canvas,r:RectF){q.color=Color.argb(230,39,50,48);c.drawRoundRect(r,5*d,5*d,q);o.color=Color.rgb(93,112,106);o.strokeWidth=d;c.drawRoundRect(r,5*d,5*d,o)}
    private fun btn(c:Canvas,id:String,r:RectF,label:String,on:Boolean=false){hits[id]=RectF(r);q.color=if(on)Color.rgb(67,113,110) else Color.rgb(57,75,72);c.drawRoundRect(r,4*d,4*d,q);o.color=Color.rgb(106,126,120);o.strokeWidth=d;c.drawRoundRect(r,4*d,4*d,o);text(c,label,r.centerX(),r.centerY()+5*d,if(r.height()>45*d)15f else 12f,Color.WHITE,Paint.Align.CENTER,on)}
    private fun top(c:Canvas,title:String){q.color=Color.rgb(75,101,98);c.drawRect(0f,0f,width.toFloat(),43*d,q);text(c,title,width/2f,28*d,18f,Color.WHITE,Paint.Align.CENTER,true);btn(c,"back",RectF(8*d,6*d,66*d,37*d),t("رجوع","Back"))}
    private fun path(a:FloatArray)=Path().apply{moveTo(a[0],a[1]);var i=2;while(i<a.size){lineTo(a[i],a[i+1]);i+=2};close()}
    private fun flag(c:Canvas,k:String,x:Float,y:Float,r:Float){q.color=col[k]?:Color.DKGRAY;c.drawCircle(x,y,r,q);o.color=Color.LTGRAY;o.strokeWidth=d;c.drawCircle(x,y,r,o);text(c,k.take(2),x,y+4*d,8f,Color.WHITE,Paint.Align.CENTER,true)}

    private fun home(c:Canvas){
        q.shader=LinearGradient(0f,0f,width.toFloat(),height.toFloat(),Color.rgb(27,59,60),Color.rgb(18,24,25),Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),q);q.shader=null
        text(c,t("إمبراطوريتنا","OUR REALM"),width/2f,height*.23f,34f,Color.WHITE,Paint.Align.CENTER,true);text(c,t("استراتيجية • سياسة • اقتصاد • تاريخ بديل","Strategy • Politics • Economy • Alternate History"),width/2f,height*.31f,14f,Color.rgb(191,211,206),Paint.Align.CENTER)
        val w=min(width*.42f,380*d);val l=width/2f-w/2f;btn(c,"new",RectF(l,height*.46f,l+w,height*.46f+52*d),t("لعبة جديدة","New Game"),true);btn(c,"continue",RectF(l,height*.46f+64*d,l+w,height*.46f+116*d),t("متابعة","Continue"));btn(c,"settings",RectF(l,height*.46f+128*d,l+w,height*.46f+180*d),t("الإعدادات واللغة","Settings & Language"))
        text(c,"v0.1",width/2f,height-18*d,11f,Color.GRAY,Paint.Align.CENTER)
    }

    private fun setup(c:Canvas){
        top(c,t("إعداد اللعبة الجديدة","New Game Setup"));val m=18*d;val y0=58*d;val l=RectF(m,y0,width*.63f,height-m);val r=RectF(width*.65f,y0,width-m,height-m);box(c,l);box(c,r)
        text(c,t("اختر الحقبة / السنة","Choose Era / Year"),l.left+16*d,y0+28*d,16f,Color.WHITE,Paint.Align.LEFT,true)
        val ys=intArrayOf(1700,1812,1914,1936,1941,1960,2023);val g=7*d;val w=(l.width()-32*d-g*3)/4;for(i in ys.indices){val row=i/4;val co=i%4;val x=l.left+16*d+co*(w+g);val y=y0+42*d+row*(48*d+g);btn(c,"year_${ys[i]}",RectF(x,y,x+w,y+48*d),ys[i].toString(),year==ys[i])}
        val y2=y0+160*d;text(c,t("نوع اللعبة","Game Mode"),l.left+16*d,y2,15f,Color.WHITE,Paint.Align.LEFT,true);val ms=arrayOf(t("لاعب واحد","Single Player"),t("عدة لاعبين","Hot-seat"),t("متفرج","Observer"));val mw=(l.width()-40*d)/3;for(i in 0..2)btn(c,"mode_$i",RectF(l.left+12*d+i*mw,y2+12*d,l.left+8*d+(i+1)*mw,y2+58*d),ms[i],mode==i)
        val y3=y2+92*d;text(c,t("الصعوبة","Difficulty"),l.left+16*d,y3,15f,Color.WHITE,Paint.Align.LEFT,true);val ds=arrayOf(t("سهلة","Easy"),t("متوسطة","Normal"),t("صعبة","Hard"));for(i in 0..2)btn(c,"diff_$i",RectF(l.left+12*d+i*mw,y3+12*d,l.left+8*d+(i+1)*mw,y3+56*d),ds[i],diff==i)
        text(c,t("الدولة","Country"),r.centerX(),y0+28*d,16f,Color.WHITE,Paint.Align.CENTER,true);flag(c,me,r.centerX(),y0+75*d,28*d);text(c,n(me),r.centerX(),y0+120*d,17f,Color.WHITE,Paint.Align.CENTER,true)
        val cs=if(year<=1914)arrayOf("YEM","GBR","FRA","ESP","OTT","RUS")else arrayOf("YEM","GBR","FRA","DEU","ITA","RUS");var cy=y0+138*d;for(k in cs){btn(c,"country_$k",RectF(r.left+14*d,cy,r.right-14*d,cy+35*d),n(k),me==k);cy+=39*d}
        btn(c,"start",RectF(r.left+14*d,r.bottom-56*d,r.right-14*d,r.bottom-12*d),t("ابدأ السيناريو","Start Scenario"),true);text(c,t("تتغير الحدود والاستعمار حسب السنة المختارة","Borders and colonial ownership change with the year"),l.left+16*d,l.bottom-17*d,11f,Color.LTGRAY)
    }

    private fun owner(r:R)=when(year){1700->when(r.id){"turkey","levant","iraq","egypt","balkans","greece","algeria","tunisia"->"OTT";else->r.owner};1812->when(r.id){"france","germany","italy"->"NAP";"turkey","levant","iraq","egypt","balkans","greece"->"OTT";else->r.owner};1914->when(r.id){"turkey","levant","iraq","balkans"->"OTT";"egypt"->"COL_GBR";"algeria","tunisia"->"COL_FRA";"libya"->"ITA";else->r.owner};1936->when(r.id){"egypt"->"COL_GBR";"algeria","tunisia","morocco"->"COL_FRA";"libya"->"COL_ITA";else->r.owner};1941->when(r.id){"germany","poland"->"GER41";"libya"->"COL_ITA";"egypt"->"COL_GBR";"algeria","morocco"->"COL_FRA";else->r.owner};1960->if(r.id=="algeria")"COL_FRA" else r.owner;else->r.owner}
    private fun trans():Triple<Float,Float,Float>{val b=min(width/1200f,(height-78*d)/760f);val z=b*zoom;return Triple(z,(width-1200*z)/2+panX,(height-700*z)/2+10*d+panY)}

    private fun map(c:Canvas){q.color=Color.rgb(38,96,106);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),q);val(a,x,y)=trans();c.save();c.translate(x,y);c.scale(a,a);q.color=Color.rgb(40,100,110);c.drawRect(-300f,-200f,1500f,900f,q);for(r in rs){val k=owner(r);q.color=col[k]?:Color.GRAY;c.drawPath(path(r.p),q);o.color=Color.rgb(31,42,40);o.strokeWidth=max(1.1f,2.2f/zoom);c.drawPath(path(r.p),o);val cc=r.c();if(zoom<2f)raw(c,if(ar)r.ar else r.en,cc.x,cc.y,12f/max(.8f,zoom));else{raw(c,if(ar)r.cityAr else r.cityEn,cc.x,cc.y,10f/zoom);detail(c,r,cc)}};c.restore();resources(c);bottom(c);selected?.let{info(c,it)}}
    private fun raw(c:Canvas,z:String,x:Float,y:Float,sz:Float){q.color=Color.WHITE;q.textSize=sz;q.textAlign=Paint.Align.CENTER;q.typeface=Typeface.DEFAULT;c.drawText(z,x,y,q)}
    private fun detail(c:Canvas,r:R,cc:PointF){if(zoom<2f)return;o.color=Color.argb(150,47,60,57);o.strokeWidth=max(.6f,1.2f/zoom);var i=0;while(i<r.p.size){val j=(i+4)%r.p.size;c.drawLine((r.p[i]+cc.x)/2,(r.p[i+1]+cc.y)/2,(r.p[j]+cc.x)/2,(r.p[j+1]+cc.y)/2,o);i+=4};if(zoom>2.6f){q.color=Color.rgb(232,206,128);c.drawCircle(cc.x,cc.y-9/zoom,3.2f/zoom,q)}}
    private fun resources(c:Canvas){q.color=Color.argb(230,40,54,51);c.drawRect(0f,0f,width.toFloat(),48*d,q);flag(c,me,29*d,24*d,18*d);val a=arrayOf("● $gold","▲ $food","╱ $recruits","⚔ $armies","★ $stability%",t("الدور $turn","Turn $turn"));var x=58*d;for(z in a){text(c,z,x,30*d,12f);x+=82*d};btn(c,"end",RectF(width-112*d,7*d,width-10*d,42*d),t("إنهاء الدور","End Turn"),true)}
    private fun bottom(c:Canvas){val h=54*d;val y=height-h;q.color=Color.argb(230,35,47,45);c.drawRect(0f,y,width.toFloat(),height.toFloat(),q);val ids=arrayOf("nav_country","nav_dip","nav_army","nav_org","nav_settings");val ls=arrayOf(t("الدولة","Country"),t("الدبلوماسية","Diplomacy"),t("الجيش","Army"),t("المنظمات","Organizations"),t("الإعدادات","Settings"));val bw=min(150*d,(width-16*d)/5);var x=(width-bw*5)/2;for(i in ids.indices){btn(c,ids[i],RectF(x+3*d,y+7*d,x+bw-3*d,height-7*d),ls[i]);x+=bw}}
    private fun info(c:Canvas,r:R){val z=RectF(12*d,58*d,280*d,151*d);box(c,z);val k=owner(r);flag(c,k,z.left+28*d,z.top+28*d,17*d);text(c,if(ar)r.ar else r.en,z.left+55*d,z.top+23*d,15f,Color.WHITE,Paint.Align.LEFT,true);text(c,t("المالك: ","Owner: ")+n(k),z.left+55*d,z.top+47*d,12f,Color.LTGRAY);text(c,t("المدينة: ","City: ")+(if(ar)r.cityAr else r.cityEn),z.left+12*d,z.top+70*d,11f,Color.LTGRAY);btn(c,"region",RectF(z.right-88*d,z.bottom-33*d,z.right-8*d,z.bottom-6*d),t("تفاصيل","Details"))}

    private fun bg(c:Canvas){q.color=Color.rgb(33,73,76);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),q)}
    private fun country(c:Canvas){bg(c);top(c,t("إدارة الدولة","Country Management"));val l=RectF(24*d,62*d,width*.31f,height-18*d);val r=RectF(width*.32f,62*d,width-24*d,height-18*d);box(c,l);box(c,r);text(c,n(me),l.centerX(),l.top+35*d,21f,Color.WHITE,Paint.Align.CENTER,true);flag(c,me,l.centerX(),l.top+90*d,34*d);val st=arrayOf(t("الاستقرار","Stability")+": $stability%",t("الذهب","Gold")+": $gold",t("المؤن","Provisions")+": $food",t("المجندون","Recruits")+": $recruits",t("الجيوش","Armies")+": $armies");var y=l.top+145*d;for(z in st){text(c,z,l.left+20*d,y,13f,Color.LTGRAY);y+=29*d};text(c,t("الحكومة","Government"),r.centerX(),r.top+35*d,20f,Color.WHITE,Paint.Align.CENTER,true);text(c,t("النظام السياسي قابل للتغيير ويؤثر على الاستقرار والاقتصاد","Government affects stability and economy"),r.left+22*d,r.top+80*d,13f,Color.LTGRAY);val w=(r.width()-54*d)/2;btn(c,"gov",RectF(r.left+18*d,r.top+150*d,r.left+18*d+w,r.top+194*d),t("تغيير الحكومة","Change Government"));btn(c,"tax",RectF(r.left+36*d+w,r.top+150*d,r.right-18*d,r.top+194*d),t("تغيير الضرائب","Change Taxes"));btn(c,"capital",RectF(r.left+18*d,r.top+210*d,r.left+18*d+w,r.top+254*d),t("تغيير العاصمة","Change Capital"));btn(c,"color",RectF(r.left+36*d+w,r.top+210*d,r.right-18*d,r.top+254*d),t("لون الدولة","Country Color"))}
    private fun dip(c:Canvas){bg(c);top(c,t("الدبلوماسية","Diplomacy"));val z=RectF(28*d,65*d,width-28*d,height-20*d);box(c,z);val left=z.width()*.25f;flag(c,me,z.left+left/2,z.top+92*d,31*d);text(c,n(me),z.left+left/2,z.top+145*d,18f,Color.WHITE,Paint.Align.CENTER,true);val k=if(target==me)"FRA" else target;flag(c,k,z.right-left/2,z.top+92*d,31*d);text(c,n(k),z.right-left/2,z.top+145*d,18f,Color.WHITE,Paint.Align.CENTER,true);text(c,t("العروض الدبلوماسية","Diplomatic Offers"),z.centerX(),z.top+34*d,16f,Color.WHITE,Paint.Align.CENTER,true);val a=arrayOf(t("تجارة","Trade"),t("عدم اعتداء","Non-aggression Pact"),t("طلب ذهب 500","Request 500 Gold"),t("تحالف","Alliance"),t("مرور عسكري","Military Access"),t("إعلان حرب","Declare War"));var y=z.top+58*d;for(i in a.indices){btn(c,"dip_$i",RectF(z.left+left+15*d,y,z.right-left-15*d,y+35*d),a[i],i==0);y+=40*d}}
    private fun army(c:Canvas){bg(c);top(c,t("الجيش","Army"));val z=RectF(22*d,64*d,width-22*d,height-18*d);box(c,z);text(c,t("جيش المنطقة","Army of the Region"),z.centerX(),z.top+30*d,18f,Color.WHITE,Paint.Align.CENTER,true);val us=arrayOf(t("مشاة","Infantry"),t("مشاة","Infantry"),t("مدفعية","Artillery"),t("دبابة","Tank"),t("فرسان","Cavalry"),t("طيران","Air Wing"));val w=min(110*d,(z.width()-42*d)/6);var x=z.left+18*d;for((i,u) in us.withIndex()){val r=RectF(x,z.top+55*d,x+w,z.top+185*d);q.color=Color.rgb(72,84,79);c.drawRect(r,q);text(c,if(i==3)"▰" else if(i==5)"✦" else "♟",r.centerX(),r.top+62*d,29f,Color.WHITE,Paint.Align.CENTER,true);text(c,u,r.centerX(),r.bottom-24*d,12f,Color.WHITE,Paint.Align.CENTER);x+=w+5*d};btn(c,"recruit",RectF(z.centerX()-120*d,z.bottom-58*d,z.centerX()+120*d,z.bottom-14*d),t("تجنيد وحدة جديدة","Recruit New Unit"),true)}
    private fun org(c:Canvas){bg(c);top(c,t("المنظمات","Organizations"));val l=RectF(28*d,65*d,width*.38f,height-20*d);val r=RectF(width*.39f,65*d,width-28*d,height-20*d);box(c,l);box(c,r);val a=if(year>=1960)arrayOf(t("الأمم المتحدة","United Nations"),t("جامعة الدول العربية","Arab League"),t("الاتحاد الأوروبي","European Union"),"NATO")else arrayOf(t("تحالف تاريخي","Historical Alliance"),t("وفاق إقليمي","Regional Entente"),t("معاهدة دفاع","Defense Pact"));var y=l.top+20*d;for(i in a.indices){btn(c,"org_$i",RectF(l.left+12*d,y,l.right-12*d,y+38*d),a[i],i==0);y+=43*d};text(c,a[0],r.centerX(),r.top+38*d,20f,Color.WHITE,Paint.Align.CENTER,true);text(c,t("الأعضاء والعروض والانضمام والخروج والدفاع المشترك","Members, offers, joining, leaving and mutual defense"),r.left+22*d,r.top+90*d,13f,Color.LTGRAY);var x=r.left+35*d;for(k in arrayOf("GBR","FRA","ESP","DEU","ITA","YEM","EGY")){flag(c,k,x,r.top+135*d,15*d);x+=43*d};btn(c,"org_offer",RectF(r.left+22*d,r.bottom-58*d,r.right-22*d,r.bottom-14*d),t("إرسال عرض للمنظمة","Send an Offer"),true)}
    private fun settings(c:Canvas){top(c,t("الإعدادات","Settings"));val z=RectF(width*.2f,70*d,width*.8f,height-30*d);box(c,z);text(c,t("اللغة","Language"),z.left+24*d,z.top+42*d,17f,Color.WHITE,Paint.Align.LEFT,true);val w=(z.width()-60*d)/2;btn(c,"ar",RectF(z.left+20*d,z.top+58*d,z.left+20*d+w,z.top+108*d),"العربية",ar);btn(c,"en",RectF(z.left+40*d+w,z.top+58*d,z.right-20*d,z.top+108*d),"English",!ar);text(c,t("الخريطة تعرض تفاصيل أكثر كلما كبرت: دول ← أقاليم ← مدن","The map reveals more detail as you zoom: countries → regions → cities"),z.left+24*d,z.top+170*d,13f,Color.LTGRAY);btn(c,"reset",RectF(z.left+20*d,z.top+200*d,z.right-20*d,z.top+244*d),t("إعادة ضبط الخريطة","Reset Map Zoom & Pan"))}

    private fun tap(x:Float,y:Float){for((k,r) in hits)if(r.contains(x,y)){act(k);return};if(s==S.MAP&&y>48*d&&y<height-54*d){val(a,ox,oy)=trans();val mx=(x-ox)/a;val my=(y-oy)/a;selected=rs.lastOrNull{it.hit(mx,my)};selected?.let{target=owner(it)};invalidate()}}
    private fun act(k:String){when{ k=="new"->s=S.SETUP; k=="continue"&&started->s=S.MAP; k=="settings"->s=S.SETTINGS; k=="back"->s=if(s==S.SETUP||s==S.SETTINGS&&!started)S.HOME else S.MAP; k.startsWith("year_")->year=k.substringAfter("year_").toInt(); k.startsWith("mode_")->mode=k.substringAfter("mode_").toInt(); k.startsWith("diff_")->diff=k.substringAfter("diff_").toInt(); k.startsWith("country_")->me=k.substringAfter("country_"); k=="start"->{started=true;resetGame();s=S.MAP}; k=="end"->{turn++;gold+=420+diff*80;food+=60;recruits+=35;if(turn%3==0)stability=(stability-1).coerceAtLeast(30)}; k=="nav_country"||k=="region"->s=S.COUNTRY; k=="nav_dip"->s=S.DIP; k=="nav_army"->s=S.ARMY; k=="nav_org"->s=S.ORG; k=="nav_settings"->s=S.SETTINGS; k=="ar"->ar=true;k=="en"->ar=false;k=="reset"->{zoom=1f;panX=0f;panY=0f};k=="recruit"&&recruits>=100->{recruits-=100;gold=(gold-250).coerceAtLeast(0);armies++};k=="tax"->{gold+=200;stability=(stability-2).coerceAtLeast(20)};k=="gov"->stability=(stability+1).coerceAtMost(100);k=="dip_0"->gold+=80;k=="dip_5"->stability=(stability-3).coerceAtLeast(20)};invalidate()}
    private fun resetGame(){turn=1;gold=if(year<1900)2800 else 4500;food=700;recruits=850;armies=3;stability=86;zoom=1f;panX=0f;panY=0f;selected=null;target=me}
}
