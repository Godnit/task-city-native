// Visual identity overrides — loaded after app.js
applySettings=function(){
 document.body.classList.toggle('light',settings.theme==='light');
 const accents=[['#00c2ff','#00d4b8'],['#1565d8','#32d8ff'],['#6f7cff','#52e5c4'],['#00a889','#74f1d2']];
 let a=accents[settings.accent%accents.length];
 document.documentElement.style.setProperty('--accent',a[0]);
 document.documentElement.style.setProperty('--accent2',a[1]);
}

header=function(sub='تعلم عملي • فهم عميق • مستقبل تقني'){
 document.getElementById('topbar').innerHTML=`<div class="brand"><div class="brandIcon"><img src="brand-icon.png" alt=""></div><div class="brandText"><div class="brandTitle">أكاديمية الشبكات والإنترنت</div><div class="brandSub">${sub}</div></div></div>
 <button class="iconBtn paletteBtn" onclick="cycleAccent()" title="تغيير لون الهوية" aria-label="تغيير اللون">◉</button><button class="iconBtn" onclick="toggleTheme()" title="الوضع" aria-label="تغيير الوضع">${settings.theme==='light'?'☾':'☀'}</button>`
}

function uiIcon(name){
 const icons={
  home:'<path d="M3 10.5 12 3l9 7.5v9A1.5 1.5 0 0 1 19.5 21h-15A1.5 1.5 0 0 1 3 19.5z"/><path d="M9 21v-6h6v6"/>',
  academy:'<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v16H6.5A2.5 2.5 0 0 0 4 21.5z"/><path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v16h4.5A2.5 2.5 0 0 1 20 21.5z"/>',
  lab:'<path d="M9 3h6"/><path d="M10 3v6l-5 9a2 2 0 0 0 1.8 3h10.4A2 2 0 0 0 19 18l-5-9V3"/><path d="M8 15h8"/>',
  challenges:'<path d="M9 4h6l1 2h3v15H5V6h3z"/><path d="m8.5 13 2 2 5-5"/>',
  progress:'<path d="M5 20V10"/><path d="M12 20V4"/><path d="M19 20v-7"/>'
 };
 return `<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${icons[name]||''}</svg>`
}

nav=function(){
 let n=[['home','الرئيسية'],['academy','الدروس'],['lab','المختبر'],['challenges','التحديات'],['progress','التقدم']];
 document.getElementById('bottomNav').innerHTML=n.map(x=>`<button class="navBtn ${current===x[0]?'active':''}" onclick="go('${x[0]}')"><b>${uiIcon(x[0])}</b><span>${x[1]}</span></button>`).join('')
}

home=function(s){
 let cs=currentStage(),sp=stageProgress(cs),next=DATA.lessons.findIndex((l,i)=>l.stage===cs&&!progress.completed.includes(i)&&isLessonUnlocked(i)),st=DATA.stages.find(x=>x.id===cs);
 s.innerHTML=`<section class="hero brandHero"><div class="heroBrandMark"><img src="brand-icon.png" alt=""></div><div class="eyebrow">مرحباً بك • المسار الكامل من الصفر</div><h1>أكاديمية الشبكات والإنترنت</h1><p class="brandTagline">تعلم عملي • فهم عميق • مستقبل تقني</p><div class="heroProgressRow"><div class="heroRing" style="--deg:${totalPct()*3.6}deg"><div><strong>${totalPct()}%</strong><span>مكتمل</span></div></div><div class="heroStage"><span>المرحلة الحالية</span><b>${st?.title||'أساسيات الشبكات'}</b><small>${sp.done} من ${sp.total} دروس مكتملة</small><div class="progressBar"><div class="progressFill" style="width:${sp.pct}%"></div></div></div></div></section>
 <div class="quickGrid"><button onclick="go('lab')"><span>${uiIcon('lab')}</span><b>مختبر الشبكات</b><small>ابنِ واختبر</small></button><button onclick="go('academy')"><span>${uiIcon('academy')}</span><b>الدروس</b><small>المسار التعليمي</small></button><button onclick="go('challenges')"><span>${uiIcon('challenges')}</span><b>تحدٍ عملي</b><small>اختبر مهارتك</small></button><button onclick="go('progress')"><span>${uiIcon('progress')}</span><b>التقدم</b><small>تابع إتقانك</small></button></div>
 <div class="sectionTitle"><h2>المراحل التعليمية</h2><span>المرحلة ${cs} من 4</span></div>
 ${DATA.stages.map(stg=>{let p=stageProgress(stg.id),u=isStageUnlocked(stg.id);return `<div class="card stageCard ${u?'':'locked'}" onclick="${u?`stageFilter=${stg.id};go('academy')`:'toast(\'أكمل اختبار المرحلة السابقة أولًا\')'}"><div class="stageRail"><span>${u?(p.pct===100?'✓':stg.id):'🔒'}</span></div><div class="stageIcon">${stg.icon}</div><div class="stageBody"><div class="between flex"><h3>${stg.title}</h3><span class="badge">${u?(p.pct+'%'):'مقفلة'}</span></div><p>${stg.desc}</p><div class="miniBar"><i style="width:${p.pct}%"></i></div></div></div>`}).join('')}
 <div class="sectionTitle"><h2>المتابعة السريعة</h2><span>${sp.done}/${sp.total}</span></div>
 ${next>=0?`<div class="card nextCard premiumCard" onclick="openLesson(${next})"><div class="nextIcon">IP</div><div class="nextCopy"><span>الموضوع الحالي</span><b>${DATA.lessons[next].title}</b><small>${DATA.lessons[next].brief}</small></div><button class="arrowAction">←</button></div>`:`<div class="card successCard">${progress.examPassed.includes(cs)?'✅ أحسنت، المرحلة مكتملة. انتقل للمستوى التالي.':`🎯 أكملت دروس المرحلة ${cs}. بقي اختبار المرحلة.`}</div>`}
 <div class="academyValues"><div><b>◈</b><span>تعلم عملي</span></div><div><b>⌬</b><span>مختبر تفاعلي</span></div><div><b>✓</b><span>تقدم حقيقي</span></div></div>`
}

academy=function(s){
 let st=DATA.stages.find(x=>x.id===stageFilter)||DATA.stages[0],p=stageProgress(st.id),indices=DATA.lessons.map((l,i)=>l.stage===st.id?i:-1).filter(i=>i>=0),allDone=p.done===p.total;
 s.innerHTML=`<div class="pageLead"><div><span>المسار التعليمي</span><h1>الدروس</h1></div><div class="pageLeadIcon">${uiIcon('academy')}</div></div><div class="pillTabs">${DATA.stages.map(x=>`<button class="pill ${x.id===stageFilter?'active':''} ${isStageUnlocked(x.id)?'':'locked'}" onclick="${isStageUnlocked(x.id)?`stageFilter=${x.id};render()`:'toast(\'هذه المرحلة مقفلة\')'}">${x.id===stageFilter?'●':'○'} المرحلة ${x.id}</button>`).join('')}</div>
 <section class="hero compact courseHero"><div class="courseIcon">${st.icon}</div><div><div class="eyebrow">المرحلة ${st.id}</div><h1>${st.title}</h1><p>${st.desc}</p></div><div class="coursePct"><strong>${p.pct}%</strong><span>مكتمل</span></div><div class="progressBar"><div class="progressFill" style="width:${p.pct}%"></div></div></section>
 <div class="sectionTitle"><h2>دروس المرحلة</h2><span>${p.done}/${p.total}</span></div>
 <div class="lessonStack">${indices.map(i=>{let l=DATA.lessons[i],u=isLessonUnlocked(i),done=progress.completed.includes(i);return `<div class="card lessonCard ${u?'':'locked'} ${done?'doneLesson':''}" onclick="${u?`openLesson(${i})`:'toast(\'أكمل الدرس السابق\')'}"><div class="lessonNum">${done?'✓':String(i-indices[0]+1).padStart(2,'0')}</div><div class="lessonInfo"><h3>${l.title}</h3><p>${l.brief}</p><div class="lessonTerms">${l.terms.slice(0,3).map(t=>`<span>${t}</span>`).join('')}</div></div><span class="lessonState">${done?'مكتمل':u?'ابدأ ←':'🔒'}</span></div>`}).join('')}</div>
 <div class="sectionTitle"><h2>اختبار المرحلة</h2><span>أسئلة جديدة</span></div><div class="card examCard ${allDone?'':'locked'}"><div class="examIcon">✓</div><div><b>اختبار إتقان مستقل</b><p class="small">أسئلة مختلفة عن أسئلة الدروس، ولا تُفتح المرحلة التالية إلا بعد النجاح.</p></div><button class="primary" ${allDone?'':'disabled'} onclick="${allDone?`startExam(${st.id})`:'toast(\'أكمل جميع الدروس أولًا\')'}">${progress.examPassed.includes(st.id)?'إعادة الاختبار':'بدء الاختبار'}</button></div>`
}

lab=function(s){
 let devices=Object.entries(DATA.devices).map(([k,d])=>`<button class="tool" onclick="LAB.add('${k}')"><b>${d.icon}</b><span>${d.label}</span></button>`).join('');
 let endpoints=LAB.state.nodes.filter(n=>!['switch','ap'].includes(n.type));
 let options=endpoints.map(n=>`<option value="${n.id}">${n.name} — ${n.ip||''}</option>`).join('');
 s.innerHTML=`<div class="pageLead labLead"><div><span>بيئة تطبيقية آمنة</span><h1>مختبر الشبكات التفاعلي</h1></div><div class="pageLeadIcon">${uiIcon('lab')}</div></div><div class="labScenario"><i></i><span>السيناريو الحالي: الشبكة التي تبنيها الآن</span><b>LIVE</b></div>
 <div class="labTop">${devices}</div><div class="labHint">اسحب الأجهزة لتحريكها، ثم اختر <b>توصيل</b> واضغط جهازين. افتح أي جهاز لضبط IP وGateway والخدمات.</div>
 <div id="labCanvas" class="labCanvasWrap"></div>
 <div class="labActions"><button id="connectMode" class="secondary" onclick="LAB.toggleConnect()">🔗 توصيل</button><button class="secondary" onclick="LAB.demo()">✨ شبكة تدريبية</button><button class="secondary" onclick="LAB.distributeDhcp()">DHCP</button><button class="secondary danger" onclick="if(confirm('مسح المعمل؟'))LAB.reset()">مسح</button></div>
 <div class="card mt labControlCard"><div class="sectionTitle inside"><h2>أدوات الشبكة</h2><span id="selectedInfo"></span></div><div class="two"><div class="field"><label>المصدر</label><select id="pingSrc">${options}</select></div><div class="field"><label>الهدف</label><select id="pingDst">${options}</select></div></div><div class="field"><label>اسم DNS للاختبار</label><input id="dnsName" value="site.local"></div>
 <div class="testGrid"><button class="labTest" onclick="LAB.ping()"><b>⌁</b><span>Ping</span></button><button class="labTest" onclick="LAB.traceroute()"><b>⌘</b><span>Traceroute</span></button><button class="labTest" onclick="LAB.resolve()"><b>◎</b><span>DNS</span></button><button class="labTest" onclick="LAB.openSite()"><b>◉</b><span>HTTP</span></button><button class="secondary wide diagnoseBtn" onclick="LAB.diagnose()">تشخيص الشبكة كاملة</button></div></div>
 <div class="eventHeader"><span>سجل الأحداث</span><small>تظهر النتائج الحقيقية هنا</small></div><pre id="labStatus" class="statusBox">ابدأ ببناء شبكة، أو اضغط «شبكة تدريبية».</pre>`;
 setTimeout(()=>{LAB.render();document.querySelectorAll('.netNode').forEach(el=>{let n=LAB.node(el.dataset.id);if(n)el.dataset.type=n.type})},0)
}

progressPage=function(s){
 let weak=[];DATA.stages.forEach(st=>{let p=stageProgress(st.id);if(isStageUnlocked(st.id)&&p.pct<100)weak.push(`${st.icon} ${st.title}: ${p.pct}%`)});
 let achievements=[
  {ok:progress.completed.length>=1,icon:'◈',title:'خطوتك الأولى',desc:'أكملت أول درس'},
  {ok:progress.completed.length>=10,icon:'IP',title:'خبير IP',desc:'أكملت 10 دروس'},
  {ok:progress.challenges.length>=3,icon:'⌬',title:'مهندس المختبر',desc:'أنجزت 3 تحديات عملية'},
  {ok:progress.examPassed.length>=2,icon:'✓',title:'متعلم متقدم',desc:'نجحت في مرحلتين'}
 ];
 s.innerHTML=`<div class="profileHeader"><div class="profileLogo"><img src="brand-icon.png" alt=""></div><div><span>ملفي التعليمي</span><h1>تقدمك في الشبكات</h1><small>متعلم شبكات • المرحلة ${currentStage()}</small></div></div>
 <section class="progressDashboard"><div class="bigRing" style="--deg:${totalPct()*3.6}deg"><div><strong>${totalPct()}%</strong><span>الإنجاز الكلي</span></div></div><div class="progressMetrics"><div><strong>${progress.completed.length}</strong><span>دروس مكتملة</span></div><div><strong>${progress.challenges.length}</strong><span>مختبرات/تحديات</span></div><div><strong>${progress.examPassed.length}</strong><span>اختبارات مراحل</span></div></div></section>
 <div class="sectionTitle"><h2>المراحل التعليمية</h2><span>${progress.examPassed.length}/4 ناجحة</span></div><div class="stageStepper">${DATA.stages.map(st=>{let p=stageProgress(st.id),state=progress.examPassed.includes(st.id)?'done':isStageUnlocked(st.id)?'current':'locked';return `<button class="${state}" onclick="${isStageUnlocked(st.id)?`stageFilter=${st.id};go('academy')`:'toast(\'المرحلة مقفلة\')'}"><i>${state==='done'?'✓':st.id}</i><b>${st.title.replace('المرحلة '+st.id+' — ','')}</b><span>${p.pct}%</span></button>`}).join('')}</div>
 <div class="sectionTitle"><h2>أحدث الإنجازات</h2><span>${achievements.filter(a=>a.ok).length}/${achievements.length}</span></div><div class="achievementGrid">${achievements.map(a=>`<div class="achievement ${a.ok?'earned':'locked'}"><i>${a.icon}</i><div><b>${a.title}</b><span>${a.desc}</span></div>${a.ok?'<em>✓</em>':'<em>🔒</em>'}</div>`).join('')}</div>
 <div class="sectionTitle"><h2>ما يحتاج متابعة</h2><span>تلقائي</span></div><div class="card followCard">${weak.length?weak.map(x=>`<p>• ${x}</p>`).join(''):'✅ لا توجد مرحلة مفتوحة ناقصة.'}</div>
 <button class="secondary danger resetBtn" onclick="resetProgress()">إعادة تقدم الأكاديمية من الصفر</button>`
}

applySettings();header();nav();render();
