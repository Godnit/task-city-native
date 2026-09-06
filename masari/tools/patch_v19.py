from pathlib import Path
import re

p = Path('masari/app/src/main/java/com/masari/personalplan/MasariV17Activity.java')
s = p.read_text(encoding='utf-8')

def must(old, new, count=1):
    global s
    if old not in s:
        raise SystemExit('v19 patch pattern missing: ' + old[:120])
    s = s.replace(old, new, count)

def must_re(pattern, repl, count=1):
    global s
    s2, n = re.subn(pattern, repl, s, count=count, flags=re.S)
    if n != count:
        raise SystemExit(f'v19 regex expected {count}, got {n}: {pattern[:100]}')
    s = s2

# Annual plan: medicine appears only during the final 90 days before the configured exam date.
must('a.add(t("medicine",570,660,medicineTitle(day),"الطب والقبول",28,true,"medicine","أساسي","تقدم مباشر نحو اختبار القبول الطبي","القبول في الطب"));',
     'if(medicineSeasonActive(d))a.add(t("medicine",570,660,medicineTitle(day),"الطب والقبول",28,true,"medicine","أساسي","تقدم مباشر نحو اختبار القبول الطبي","القبول في الطب"));')
must('a.add(t("thu_medicine",510,570,"مراجعة الطب والقبول للأسبوع","الطب والقبول",22,true,"medicine","أساسي","مراجعة الموضوعات والأسئلة ونقاط الضعف","القبول في الطب"));',
     'if(medicineSeasonActive(d))a.add(t("thu_medicine",510,570,"مراجعة الطب والقبول للأسبوع","الطب والقبول",22,true,"medicine","أساسي","مراجعة الموضوعات والأسئلة ونقاط الضعف","القبول في الطب"));')

# Practical English rather than a book/listening-centric plan.
must('a.add(t("english",420,480,englishTitle(day),"الإنجليزية",20,true,"headphones","أساسي","مفردات واستماع أو قراءة مركزة","رفع الإنجليزية"));',
     'a.add(t("english",420,480,englishTitle(day),"الإنجليزية",20,true,"talk","أساسي","إنجليزية عملية: مفردات منهجية وقواعد وفهم واستخدام","إنجليزية للدراسة والحياة والعمل"));')
must('a.add(t("thu_english",585,630,"مراجعة الإنجليزية الأسبوعية","الإنجليزية",16,true,"headphones","أساسي","مراجعة الكلمات والاستماع والقراءة","رفع الإنجليزية"));',
     'a.add(t("thu_english",585,630,"مراجعة الإنجليزية العملية للأسبوع","الإنجليزية",16,true,"talk","أساسي","مراجعة المفردات والقواعد والقراءة والاستخدام","إنجليزية للدراسة والحياة والعمل"));')

must_re(r'private String englishTitle\(int d\)\{switch\(d\)\{.*?default:return.*?;\}\}', '''private String englishTitle(int d){switch(d){case Calendar.SATURDAY:return"الإنجليزية العملية — مفردات المنهج + استخدام";case Calendar.SUNDAY:return"الإنجليزية العملية — قواعد + تمارين";case Calendar.MONDAY:return"الإنجليزية العملية — قراءة وفهم";case Calendar.TUESDAY:return"الإنجليزية العملية — مفردات + قواعد";case Calendar.WEDNESDAY:return"الإنجليزية العملية — كتابة وتعبير قصير";case Calendar.THURSDAY:return"مراجعة الإنجليزية العملية للأسبوع";default:return"إنجليزية حرة اختيارية";}}''')

# Apply persistent schedule overrides after built-in/custom tasks are assembled.
must('if(matchUsedOn(d))a=applyMatch(a,d);addCustom(a,d);return a;}',
     'if(matchUsedOn(d))a=applyMatch(a,d);addCustom(a,d);a=applyV19Overrides(a,d);return a;}')

insert_before = '    private List<Task> baseTasksFor(Calendar d)'
helper = r'''    private boolean medicineSeasonActive(Calendar d){long exam=prefs.getLong("v19_exam_date",0);if(exam<=0)return false;long when=d.getTimeInMillis(),start=exam-90L*24L*60L*60L*1000L;return when>=start&&when<=exam+24L*60L*60L*1000L;}
    private List<Task> applyV19Overrides(List<Task> base,Calendar d){List<Task>out=new ArrayList<>();for(Task t:base){int mask=prefs.getInt("v19_days_"+t.id,0);if(mask!=0&&(mask&(1<<d.get(Calendar.DAY_OF_WEEK)))==0)continue;int md=prefs.getInt("v19_monthday_"+t.id,0);if(md>0&&d.get(Calendar.DAY_OF_MONTH)!=md)continue;int start=prefs.getInt("v19_start_"+t.id,-1),dur=prefs.getInt("v19_duration_"+t.id,-1);int oldDur=Math.max(1,t.end-t.start);if(start>=0)t.start=start;if(dur>0)t.end=t.start+dur;else if(start>=0)t.end=t.start+oldDur;String title=prefs.getString("v19_title_"+t.id,"");if(!title.isEmpty())t.title=title;out.add(t);}return out;}
'''
if insert_before not in s:
    raise SystemExit('v19 insert point missing')
s = s.replace(insert_before, helper + insert_before, 1)

# Rescue/emergency modes respect the annual medicine phase.
must('a.add(t("rescue_medicine",750,775,"طب — ٢٥ دقيقة فقط","الطب والقبول",14,true,"medicine","أساسي","حافظ على الاتصال بهدف الطب","القبول في الطب"));',
     'if(medicineSeasonActive(d))a.add(t("rescue_medicine",750,775,"طب — ٢٥ دقيقة فقط","الطب والقبول",14,true,"medicine","أساسي","حافظ على الاتصال بهدف الطب","القبول في الطب"));else a.add(t("rescue_english",750,775,"إنجليزية عملية — ٢٥ دقيقة","الإنجليزية",14,true,"talk","أساسي","عودة صغيرة للمفردات والقواعد والاستخدام","إنجليزية للدراسة والحياة والعمل"));')
must('a.add(t("em_sick_medicine",760,785,"طب خفيف — ٢٥ دقيقة (إن استطعت)","الطب والقبول",10,false,"medicine","اختياري","منع الانقطاع الكامل فقط","القبول في الطب"));',
     'if(medicineSeasonActive(d))a.add(t("em_sick_medicine",760,785,"طب خفيف — ٢٥ دقيقة (إن استطعت)","الطب والقبول",10,false,"medicine","اختياري","منع الانقطاع الكامل فقط","القبول في الطب"));')
must('a.add(t("em_family_medicine",780,810,"طب — ٣٠ دقيقة","الطب والقبول",14,true,"medicine","أساسي","كتلة قصيرة للقبول","القبول في الطب"));',
     'if(medicineSeasonActive(d))a.add(t("em_family_medicine",780,810,"طب — ٣٠ دقيقة","الطب والقبول",14,true,"medicine","أساسي","كتلة قصيرة للقبول","القبول في الطب"));else a.add(t("em_family_english",780,810,"إنجليزية عملية — ٣٠ دقيقة","الإنجليزية",14,true,"talk","أساسي","مفردات أو قواعد أو قراءة مركزة","إنجليزية للدراسة والحياة والعمل"));')

# All means all: expand later/completed/overdue automatically.
must('private View tasksPage(){ScrollView s=scroll();LinearLayout r=root(s);r.addView(topBar("المهام","more","filter"));r.addView(weekStrip());if(isFriday(Calendar.getInstance())){addDayBanner(r);addFridayIdeas(r);gap(r,18);return s;}HorizontalScrollView hs=',
     'private View tasksPage(){ScrollView s=scroll();LinearLayout r=root(s);r.addView(topBar("المهام","more","filter"));r.addView(weekStrip());if(isFriday(Calendar.getInstance())){addDayBanner(r);addFridayIdeas(r);gap(r,18);return s;}if("الكل".equals(taskFilter)){laterExpanded=true;completedExpanded=true;overdueExpanded=true;}HorizontalScrollView hs=')

# Tag each rendered task so v19 can open a real task history/editor screen.
must('box.setOnClickListener(v->showTaskDialog(t));}', 'box.setTag(t.id);box.setContentDescription("task:"+t.id);box.setOnClickListener(v->showTaskDialog(t));}')

# Cancelled-for-a-valid-reason is excluded from the denominator instead of becoming a hidden failure.
must('private int dayTarget(Calendar c){if(isFriday(c))return 0;int n=0;for(Task t:tasksFor(c))if(t.required)n+=t.points;return n;}',
     'private int dayTarget(Calendar c){if(isFriday(c))return 0;int n=0;String dk=dateKey(c);for(Task t:tasksFor(c))if(t.required&&!"cancelled".equals(statusOn(t,dk)))n+=t.points;return n;}')

# Core success adapts to the phase: 3/4 outside medicine season, 4/5 in the final exam phase.
must_re(r'private int coreDoneToday\(\)\{.*?return n;\}', '''private int coreDoneToday(){if(isFriday(Calendar.getInstance()))return medicineSeasonActive(Calendar.getInstance())?5:4;if(isThursday(Calendar.getInstance()))return thursdayCoreDone();int n=0;if(coreDone("quran"))n++;if(coreDone("english"))n++;if(coreDone("work"))n++;if(coreDone("close"))n++;if(medicineSeasonActive(Calendar.getInstance())&&coreDone("medicine"))n++;return n;}''')
must_re(r'private int thursdayCoreDone\(\)\{.*?return n;\}', '''private int thursdayCoreDone(){int n=0;String[] ids=medicineSeasonActive(Calendar.getInstance())?new String[]{"thu_quran","thu_medicine","thu_english","thu_work","thu_review"}:new String[]{"thu_quran","thu_english","thu_work","thu_review"};for(String id:ids)if(hasStatus(id))n++;return n;}''')

# Reward credits become immediately understandable and usable after a successful day.
must_re(r'private void syncCredits\(\)\{.*?e\.apply\(\);\}', '''private void syncCredits(){if(isFriday(Calendar.getInstance()))return;SharedPreferences.Editor e=prefs.edit();int add=0;String dk=todayKey;boolean med=medicineSeasonActive(Calendar.getInstance());int done=coreDoneToday(),needed=med?4:3,full=med?5:4;if(done>=needed&&!prefs.getBoolean("v19_reward_success_"+dk,false)){add+=10;e.putBoolean("v19_reward_success_"+dk,true);}if(done>=full&&!prefs.getBoolean("v19_reward_full_"+dk,false)){add+=5;e.putBoolean("v19_reward_full_"+dk,true);}String wk=weekId(Calendar.getInstance());if(strongDaysThisWeek()>=4&&!prefs.getBoolean("v19_reward_week_"+wk,false)){add+=25;e.putBoolean("v19_reward_week_"+wk,true);}add+=awardOnce(e,"v19_reward_quran20",countDoneIds("quran")>=20,20);add+=awardOnce(e,"v19_reward_english20",countDoneIds("english")>=20,15);if(med)add+=awardOnce(e,"v19_reward_medicine15",countDoneIds("medicine")>=15,20);if(add>0)e.putInt("reward_credits",credits()+add);e.apply();}''')

# English result logging feeds useful monthly stats, not a listening-only metric.
old_eng = 'else if(t.domain.equals("الإنجليزية")){addResultField(v,fields,names,"كلمات جديدة","words",true);addResultField(v,fields,names,"دقائق الاستماع","listening",true);addResultField(v,fields,names,"ملاحظة قصيرة","note",false);}'
new_eng = 'else if(t.domain.equals("الإنجليزية")){addResultField(v,fields,names,"عدد المفردات/الأسئلة","words",true);addResultField(v,fields,names,"ما الذي تدربت عليه؟ قواعد / قراءة / كتابة / استخدام","topic",false);addResultField(v,fields,names,"ملاحظة أو خطأ تعلمته","note",false);}'
must(old_eng, new_eng)

# Reduce misleading inactive-domain recommendations.
must('if(!d.equals("الراحة")&&!d.equals("الدين والمسجد")){int x=weekDomainRate(d);', 'if(!d.equals("الراحة")&&!d.equals("الدين والمسجد")&&(!d.equals("الطب والقبول")||medicineSeasonActive(Calendar.getInstance()))){int x=weekDomainRate(d);', 2)

# Quick tiles match the current annual phase.
old_quick='q.addView(quickTile("الطب","medicine",CYAN),weightSmall());q.addView(quickTile("القرآن","quran",GREEN),weightSmall());q.addView(quickTile("English","headphones",BLUE),weightSmall());q.addView(quickTile("العمل","briefcase",ORANGE),weightSmall());'
new_quick='if(medicineSeasonActive(Calendar.getInstance()))q.addView(quickTile("الطب","medicine",CYAN),weightSmall());else q.addView(quickTile("القراءة","book",PURPLE),weightSmall());q.addView(quickTile("القرآن","quran",GREEN),weightSmall());q.addView(quickTile("الإنجليزية","talk",BLUE),weightSmall());q.addView(quickTile("العمل","briefcase",ORANGE),weightSmall());'
must(old_quick,new_quick)

p.write_text(s, encoding='utf-8')
print('v19 patch applied')
