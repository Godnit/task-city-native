/* Godot Academy v4b — clean complete core */
(function(){
progress.stageExams=progress.stageExams||{};
progress.stageLabs=progress.stageLabs||{};
if(progress.examPassed)progress.stageExams[1]=true;
if(progress.labPassed)progress.stageLabs[1]=true;
const KEY='godot_academy_core_v4b';
let st={stage:1,filter:'all',search:''};
try{st={...st,...JSON.parse(localStorage.getItem(KEY)||'{}')}}catch(e){}
function save(){localStorage.setItem(KEY,JSON.stringify(st));saveProgress()}
function ids(s){return DATA.lessons.map((l,i)=>l.stageId===s?i:-1).filter(i=>i>=0)}
function lessonsDone(s){const a=ids(s);return !!a.length&&a.every(i=>progress.completed.includes(i))}
function examDone(s){return s===1?!!progress.examPassed:!!progress.stageExams[s]}
function labDone(s){return s===1?!!progress.labPassed:!!progress.stageLabs[s]}
function done(s){return lessonsDone(s)&&examDone(s)&&labDone(s)}
function unlocked(s){return s===1||done(s-1)}
function pctStage(s){const a=ids(s),n=a.filter(i=>progress.completed.includes(i)).length;return Math.round((a.length?n/a.length*70:0)+(examDone(s)?15:0)+(labDone(s)?15:0))}
function high(){let h=1;for(let s=2;s<=8;s++)if(unlocked(s))h=s;return h}
function pctAll(){let n=progress.completed.length;for(let s=1;s<=8;s++){if(examDone(s))n++;if(labDone(s))n++}return Math.round(n/(DATA.lessons.length+16)*100)}
function stageName(s){return DATA.stages.find(x=>x.id===s)?.title||('مرحلة '+s)}
function nextLesson(){for(let s=1;s<=8;s++){if(!unlocked(s))break;for(const i of ids(s))if(!progress.completed.includes(i))return i}return -1}
function lessonOpen(i){const l=DATA.lessons[i];if(!l||!unlocked(l.stageId))return false;const a=ids(l.stageId),p=a.indexOf(i);return p===0||progress.completed.includes(a[p-1])}
const catMap={1:'fundamentals',2:'programming',3:'apps',4:'2d',5:'3d',6:'mobile',7:'export',8:'projects'};
const cats=[['all','الكل'],['fundamentals','الأساسيات'],['programming','البرمجة'],['apps','التطبيقات'],['2d','2D'],['3d','3D'],['mobile','الهاتف'],['export','التصدير'],['projects','المشاريع']];
const originalVisual=visual;
function picture(l){
 if(l.stageId===1)return originalVisual(l.visual);
 const stage=DATA.stages[l.stageId-1];
 const a=l.terms?.[0]||'Learn',b=l.terms?.[1]||'Build';
 return `<div class="visualCard visualV4"><div class="diagramV4"><div>${stage.icon} ${a}</div><b>→</b><div>${b}</div></div><div class="visualCaption">رسم مبسط: اربط المصطلح بما يبنيه داخل Godot.</div></div>`;
}

header=function(sub='تعلّم • ابنِ • انشر'){
 document.getElementById('topbar').innerHTML=`<div class="brand"><div class="brandLogoWrap">${typeof logoSvg==='function'?logoSvg():'◉'}</div><div class="brandText"><div class="brandTitle"><em>Godot</em> أكاديمية</div><div class="brandSub">${sub}</div></div></div><button class="iconBtn" onclick="toggleTheme()">${settings.theme==='dark'?'☼':'☾'}</button>`;
};
nav=function(){
 const items=[['home','⌂','الرئيسية'],['academy','▤','الدروس'],['lab','⌬','المعمل'],['progress','⌁','تقدمي']];
 document.getElementById('bottomNav').style.gridTemplateColumns='repeat(4,1fr)';
 document.getElementById('bottomNav').innerHTML=items.map(x=>`<button class="navBtn ${current===x[0]?'active':''}" onclick="go('${x[0]}')"><b>${x[1]}</b>${x[2]}</button>`).join('');
};
render=function(){
 const s=document.getElementById('screen');s.scrollTop=0;
 if(current==='home')home(s);else if(current==='academy')academy(s);else if(current==='lab')labPage(s);else if(current==='helper')helperPage(s);else if(current==='progress')progressPage(s);
};

home=function(s){
 const n=nextLesson(),all=pctAll(),h=high();st.stage=h;save();
 const stages=DATA.stages.map(x=>`<div class="card stageCard ${unlocked(x.id)?'':'locked'}" onclick="${unlocked(x.id)?`openStageV4(${x.id})`:`toast('أكمل المرحلة السابقة: الدروس + الاختبار + المشروع')`}"><div class="stageIcon">${x.icon}</div><div class="stageBody"><h3>${x.id}. ${x.title}</h3><p>${x.desc}</p><div class="progressBar"><div class="progressFill" style="width:${pctStage(x.id)}%"></div></div></div><span class="badge">${done(x.id)?'✓':unlocked(x.id)?pctStage(x.id)+'%':'🔒'}</span></div>`).join('');
 s.innerHTML=`<section class="hero godotHero"><div class="nodeGraph">${typeof graphSvg==='function'?graphSvg():''}</div><div class="welcome">مرحباً 👋</div><h1>من أول Node حتى تطبيق أو لعبة APK.</h1><p>8 مراحل كاملة، 96 درسًا، 8 اختبارات، و8 مشاريع عملية. المرحلة التالية لا تفتح بالقراءة فقط.</p><button class="primary heroCTA" onclick="${n>=0?`openLesson(${n})`:`openLabStageV4(8)`}">${n>=0?'واصل التعلم':'مشروع التخرج'} ↗</button><div class="progressBar"><div class="progressFill" style="width:${all}%"></div></div><div class="stats"><div class="stat"><strong>${all}%</strong><span>التقدم الكلي</span></div><div class="stat"><strong>${progress.completed.length}</strong><span>درس مكتمل</span></div><div class="stat"><strong>${h}/8</strong><span>المرحلة المتاحة</span></div></div></section><div class="sectionTitle"><h2>مسارات الأكاديمية</h2><span>8 مراحل</span></div>${stages}<div class="sectionTitle"><h2>أدوات عملية</h2><span>تعلّم بالعمل</span></div><div class="toolGrid"><button class="toolCard" onclick="go('helper')"><b>⌘ ماذا أستخدم؟</b><span>اختر الوظيفة واعرف الـNode المناسبة.</span></button><button class="toolCard" onclick="go('lab')"><b>▶ المعامل</b><span>8 مشاريع تفاعلية متدرجة.</span></button></div>`;
};

window.openStageV4=function(s){if(!unlocked(s)){toast('المرحلة مقفلة');return}st.stage=s;st.filter=catMap[s];save();current='academy';header('المرحلة '+s+' • '+stageName(s));nav();render();};
window.setFilterV4b=function(f){st.filter=f;save();academy(document.getElementById('screen'));};
window.setStageV4b=function(s){if(!unlocked(s)){toast('أكمل المرحلة السابقة أولاً');return}st.stage=s;save();academy(document.getElementById('screen'));};
window.searchV4b=function(v){st.search=v;save();renderLessonsV4b();};

academy=function(s){
 const visible=DATA.stages.filter(x=>st.filter==='all'||catMap[x.id]===st.filter);
 if(!visible.some(x=>x.id===st.stage))st.stage=visible[0]?.id||high();
 const chips=cats.map(c=>`<button class="filterChip ${st.filter===c[0]?'active':''}" onclick="setFilterV4b('${c[0]}')">${c[1]}</button>`).join('');
 const stages=visible.map(x=>`<button class="stageChipV4 ${st.stage===x.id?'active':''} ${unlocked(x.id)?'':'locked'}" onclick="setStageV4b(${x.id})">${x.icon} ${x.id}. ${x.title}<small>${unlocked(x.id)?pctStage(x.id)+'%':'🔒'}</small></button>`).join('');
 s.innerHTML=`<div class="lessonTop"><h1>الدروس والمسارات</h1><span class="badge">${progress.completed.length}/${DATA.lessons.length}</span></div><div class="filterRow">${chips}</div><div class="searchBoxV4"><span>⌕</span><input value="${esc(st.search||'')}" oninput="searchV4b(this.value)" placeholder="ابحث في الدروس..."></div><div class="stageStripV4">${stages}</div><div id="lessonsV4b"></div>`;
 renderLessonsV4b();
};
window.renderLessonsV4b=function(){
 const r=document.getElementById('lessonsV4b');if(!r)return;
 const s=st.stage,a=ids(s),q=(st.search||'').trim().toLowerCase();
 const list=a.filter(i=>!q||DATA.lessons[i].title.toLowerCase().includes(q)||DATA.lessons[i].brief.toLowerCase().includes(q));
 const cards=list.map(i=>{const l=DATA.lessons[i],ok=lessonOpen(i),d=progress.completed.includes(i),num=a.indexOf(i)+1;return `<div class="card lessonCard ${ok?'':'locked'}" onclick="${ok?`openLesson(${i})`:`toast('أكمل الدرس السابق أولاً')`}"><div class="lessonNum">${d?'✓':String(num).padStart(2,'0')}</div><div class="lessonInfo"><h3>${l.title}</h3><p>${l.brief}</p></div><span class="badge">${d?'مكتمل':ok?'ابدأ':'🔒'}</span></div>`}).join('');
 r.innerHTML=`<div class="card stageIntroV4 ${unlocked(s)?'':'locked'}"><div class="stageIcon">${DATA.stages[s-1].icon}</div><div><h2>${stageName(s)}</h2><p>${DATA.stages[s-1].desc}</p><div class="progressBar"><div class="progressFill" style="width:${pctStage(s)}%"></div></div></div></div><div class="sectionTitle"><h2>دروس المرحلة</h2><span>${a.filter(i=>progress.completed.includes(i)).length}/${a.length}</span></div>${cards||'<div class="card emptyV4">لا توجد نتائج مطابقة.</div>'}<div class="stageRequirementsV4"><button class="reqCardV4 ${labDone(s)?'done':''}" onclick="openLabStageV4(${s})"><b>⌬ مشروع المرحلة</b><span>${labDone(s)?'مكتمل ✓':unlocked(s)?'افتح المعمل':'مقفل'}</span></button><button class="reqCardV4 ${examDone(s)?'done':''}" onclick="startExamV4b(${s})"><b>▤ اختبار المرحلة</b><span>${examDone(s)?'ناجح ✓':lessonsDone(s)?'ابدأ':'أكمل الدروس'}</span></button></div>${done(s)&&s<8?`<button class="primary" onclick="openStageV4(${s+1})">فتح المرحلة ${s+1}: ${stageName(s+1)} ←</button>`:''}`;
};

openLesson=function(i){
 if(!lessonOpen(i)){toast('الدرس مقفل');return}
 lessonIndex=i;current='lesson';const l=DATA.lessons[i],a=ids(l.stageId),num=a.indexOf(i)+1;
 header(`المرحلة ${l.stageId} • الدرس ${num}/${a.length}`);nav();
 document.getElementById('screen').innerHTML=`<article class="lessonPage"><button class="backBtn" onclick="openStageV4(${l.stageId})">→ رجوع للمسار</button><div class="crumb">${stageName(l.stageId)} / الدرس ${num}</div><h1>${l.title}</h1><p class="small">${l.brief}</p><div class="terms">${l.terms.map(t=>`<span class="term">${t}</span>`).join('')}</div>${picture(l)}<div class="lessonBody">${l.body}</div><div class="quiz"><h3>اختبر فهمك</h3><p>${l.q}</p>${l.opts.map((o,k)=>`<button class="option" onclick="answerV4b(${i},${k},this)">${o}</button>`).join('')}</div></article>`;
};
window.answerV4b=function(i,k,btn){
 const l=DATA.lessons[i],opts=[...document.querySelectorAll('.quiz .option')];opts.forEach(x=>x.disabled=true);
 if(k===l.ans){btn.classList.add('correct');if(!progress.completed.includes(i)){progress.completed.push(i);progress.completed.sort((a,b)=>a-b);saveProgress()}toast('صحيح ✓');const a=ids(l.stageId),p=a.indexOf(i);setTimeout(()=>{if(p<a.length-1)openLesson(a[p+1]);else openStageV4(l.stageId)},600)}
 else{btn.classList.add('wrong');opts[l.ans]?.classList.add('correct');toast('راجع الفكرة والمثال');setTimeout(()=>opts.forEach(x=>{x.disabled=false;x.classList.remove('wrong','correct')}),900)}
};

const originalExam=startExam;
window.startExamV4b=function(s){
 if(!unlocked(s)){toast('المرحلة مقفلة');return}if(!lessonsDone(s)){toast('أكمل جميع دروس المرحلة أولاً');return}if(s===1){originalExam();return}
 const bank=GODOT_EXAMS_V4[s];
 modal(`<h2>اختبار المرحلة ${s}</h2><p class="small">5 أسئلة جديدة مختلفة عن أسئلة الدروس. تحتاج 4/5.</p>${bank.map((q,i)=>`<div class="quiz"><b>${i+1}. ${q[0]}</b>${q[1].map((o,k)=>`<label class="option"><input type="radio" name="vb${i}" value="${k}"> ${o}</label>`).join('')}</div>`).join('')}<button class="primary" onclick="finishExamV4b(${s})">تصحيح الاختبار</button>`);
};
window.finishExamV4b=function(s){
 const bank=GODOT_EXAMS_V4[s];let score=0;
 bank.forEach((q,i)=>{const e=document.querySelector(`input[name=vb${i}]:checked`);if(e&&+e.value===q[2])score++});
 if(score>=4){progress.stageExams[s]=true;saveProgress();closeModal();toast(`نجحت ${score}/5 ✓`);setTimeout(()=>openStageV4(s),350)}else toast(`نتيجتك ${score}/5 — تحتاج 4/5`);
};

progressPage=function(s){
 const all=pctAll(),h=high();
 const stages=DATA.stages.map(x=>`<div class="card progressStageV4 ${unlocked(x.id)?'':'locked'}" onclick="${unlocked(x.id)?`openStageV4(${x.id})`:''}"><div class="stageIcon">${x.icon}</div><div class="stageBody"><h3>${x.id}. ${x.title}</h3><div class="miniChecksV4"><span class="${lessonsDone(x.id)?'done':''}">دروس ${lessonsDone(x.id)?'✓':'○'}</span><span class="${examDone(x.id)?'done':''}">اختبار ${examDone(x.id)?'✓':'○'}</span><span class="${labDone(x.id)?'done':''}">مشروع ${labDone(x.id)?'✓':'○'}</span></div><div class="progressBar"><div class="progressFill" style="width:${pctStage(x.id)}%"></div></div></div><span class="badge">${done(x.id)?'✓':unlocked(x.id)?pctStage(x.id)+'%':'🔒'}</span></div>`).join('');
 s.innerHTML=`<div class="levelHero"><div class="levelInfo"><small>مستواك الحالي</small><h2>${done(8)?'مطور Godot — خريج':'متعلم Godot — المرحلة '+h}</h2><p class="small">التقدم = الدروس + الاختبارات + المشاريع.</p><div class="progressBar"><div class="progressFill" style="width:${all}%"></div></div></div><div class="levelBadge"><div class="hex">${done(8)?'★':h}</div></div></div><div class="sectionTitle"><h2>نظرة عامة</h2><span>${all}%</span></div><div class="overviewGrid"><div class="overviewItem"><b>${progress.completed.length}</b><span>درس</span></div><div class="overviewItem"><b>${[1,2,3,4,5,6,7,8].filter(examDone).length}</b><span>اختبار</span></div><div class="overviewItem"><b>${[1,2,3,4,5,6,7,8].filter(labDone).length}</b><span>مشروع</span></div></div><div class="sectionTitle"><h2>المراحل</h2><span>8 مراحل</span></div>${stages}${done(8)?'<div class="card graduationV4"><b>🏆 أكملت أكاديمية Godot</b><p>أنهيت 96 درسًا و8 اختبارات و8 مشاريع. احتفظ بمشروع التخرج وبقية مشاريعك كمحفظة عملية.</p></div>':''}`;
};
window.appBack=function(){if(document.getElementById('modalRoot').innerHTML){closeModal();return true}if(current==='lesson'){openStageV4(DATA.lessons[lessonIndex]?.stageId||st.stage);return true}if(current!=='home'){go('home');return true}return false};
st.stage=Math.min(st.stage||1,high());save();header();nav();render();
})();