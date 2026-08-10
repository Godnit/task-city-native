/* Godot Academy v3 interaction fixes: real lesson filters/search + functional lab tabs. */
(function(){
let academyFilterV3='all';
let academySearchV3='';
let academySearchOpenV3=false;
let labTabV3='scene';

const trackDataV3={
  basics:{title:'أساسيات Godot',desc:'الواجهة، العقد، المشاهد، Inspector، Signals وأول تطبيق.',icon:'godot',locked:false,topics:['المحرر والمشاهد','Nodes وControl','Inspector وSignals','أول تطبيق Counter']},
  programming:{title:'البرمجة بـ GDScript',desc:'المتغيرات، الشروط، الدوال والقوائم والمنطق العملي.',icon:'{ }',locked:true,topics:['المتغيرات وأنواع البيانات','if / elif / else','الدوال وإرجاع القيم','Arrays وDictionaries','Signals مع الكود']},
  '2d':{title:'تطوير ألعاب 2D',desc:'الحركة والتصادم والكاميرا والأنيميشن ومشاريع كاملة.',icon:'🎮',locked:true,topics:['CharacterBody2D','الحركة والـInput','Collision وPhysics','Camera2D','Animation ومشروع لعبة']},
  '3d':{title:'تطوير ألعاب 3D',desc:'العالم ثلاثي الأبعاد والفيزياء والإضاءة والكاميرا.',icon:'◆',locked:true,topics:['Node3D وMesh','CharacterBody3D','Camera3D','Lighting','Physics ومشروع 3D']}
};

function escapeHtmlV3(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function filterLabelV3(f){return ({all:'الكل',basics:'الأساسيات',programming:'البرمجة','2d':'2D','3d':'3D'})[f]||f}
function setAcademyFilterV3(f){academyFilterV3=f; academySearchV3=''; academySearchOpenV3=false; const s=document.getElementById('screen'); if(s){academy(s); s.scrollTop=0}}
function toggleAcademySearchV3(){academySearchOpenV3=!academySearchOpenV3; const s=document.getElementById('screen'); if(s)academy(s); if(academySearchOpenV3)setTimeout(()=>document.getElementById('academySearchV3')?.focus(),40)}
function setAcademySearchV3(v){academySearchV3=v; const s=document.getElementById('screen'); if(s)academy(s); setTimeout(()=>{const e=document.getElementById('academySearchV3');if(e){e.focus();e.setSelectionRange(e.value.length,e.value.length)}},0)}
function clearAcademySearchV3(){academySearchV3=''; const s=document.getElementById('screen'); if(s)academy(s)}

window.setAcademyFilterV3=setAcademyFilterV3;
window.toggleAcademySearchV3=toggleAcademySearchV3;
window.setAcademySearchV3=setAcademySearchV3;
window.clearAcademySearchV3=clearAcademySearchV3;

function trackCardV3(key,p){
 const t=trackDataV3[key],isBasics=key==='basics';
 const progressValue=isBasics?p:0;
 const icon=t.icon==='godot'?(typeof logoSvg==='function'?logoSvg():'◉'):t.icon;
 return `<div class="card trackCard ${t.locked?'locked':''}" ${t.locked?`onclick="toast('هذا المسار سيُفتح بعد إكمال المراحل السابقة')"`:''}><div class="trackBadge">${icon}</div><div><h3>${t.title}</h3><p>${t.desc}</p></div><div class="trackProgress" style="--tp:${progressValue}%"><span>${progressValue}%</span></div></div>`
}
function roadmapV3(key){
 const t=trackDataV3[key];
 return `<div class="roadmapV3"><div class="roadmapHeadV3"><b>${t.title}</b><span>مسار قادم 🔒</span></div>${t.topics.map((x,i)=>`<div class="roadmapItemV3"><span>${String(i+1).padStart(2,'0')}</span><div><b>${x}</b><small>يفتح تدريجيًا بعد إكمال المتطلبات السابقة.</small></div><i>🔒</i></div>`).join('')}</div>`
}

academy=function(s){
 const p=pct(),q=academySearchV3.trim().toLowerCase();
 const visibleTracks=(academyFilterV3==='all'?['basics','programming','2d','3d']:[academyFilterV3]).filter(k=>{if(!q)return true;const t=trackDataV3[k];return (t.title+' '+t.desc).toLowerCase().includes(q)});
 const lessonMatches=DATA.lessons.map((l,i)=>({l,i})).filter(({l})=>!q||(l.title+' '+l.brief+' '+(l.terms||[]).join(' ')).toLowerCase().includes(q));
 const showBasics=academyFilterV3==='all'||academyFilterV3==='basics';
 s.innerHTML=`
 <div class="lessonTop"><h1>الدروس</h1><button class="iconBtn ${academySearchOpenV3?'activeSearchV3':''}" onclick="toggleAcademySearchV3()" aria-label="بحث">⌕</button></div>
 ${academySearchOpenV3?`<div class="searchBarV3"><span>⌕</span><input id="academySearchV3" type="search" placeholder="ابحث في الدروس والمسارات..." value="${escapeHtmlV3(academySearchV3)}" oninput="setAcademySearchV3(this.value)"><button onclick="clearAcademySearchV3()">×</button></div>`:''}
 <div class="filterRow">${[['all','الكل'],['basics','الأساسيات'],['programming','البرمجة'],['2d','2D'],['3d','3D']].map(([k,t])=>`<button class="filterChip ${academyFilterV3===k?'active':''}" onclick="setAcademyFilterV3('${k}')">${t}</button>`).join('')}</div>
 <div class="filterStatusV3"><span>${filterLabelV3(academyFilterV3)}</span>${q?`<b>نتائج البحث: ${escapeHtmlV3(academySearchV3)}</b>`:'<b>اختر مسارًا لعرض محتواه</b>'}</div>
 <div class="sectionTitle"><h2>المسارات التعليمية</h2><span>${visibleTracks.length} مسار</span></div>
 ${visibleTracks.length?visibleTracks.map(k=>trackCardV3(k,p)).join(''):`<div class="emptyStateV3">لا توجد نتائج مطابقة. جرّب كلمة أخرى.</div>`}
 ${showBasics?`<div class="sectionTitle"><h2>دروس الأساسيات</h2><span>${progress.completed.length}/${DATA.lessons.length}</span></div>
 ${lessonMatches.length?lessonMatches.map(({l,i})=>{let u=unlocked(i),d=progress.completed.includes(i);return `<div class="card lessonCard ${u?'':'locked'}" onclick="${u?`openLesson(${i})`:`toast('أكمل الدرس السابق أولًا')`}"><div class="lessonNum">${d?'✓':String(i+1).padStart(2,'0')}</div><div class="lessonInfo"><h3>${l.title}</h3><p>${l.brief}</p></div><span class="badge">${d?'100%':u?'ابدأ':'🔒'}</span></div>`}).join(''):`<div class="emptyStateV3">لا يوجد درس مطابق لبحثك في الأساسيات.</div>`}
 <div class="sectionTitle"><h2>اختبار المرحلة</h2><span>أسئلة جديدة</span></div><div class="card ${progress.completed.length===DATA.lessons.length?'':'locked'}"><b>اختبار فهم مستقل</b><p class="small">لا يكرر أسئلة نهاية الدروس، ويفتح جاهزية المرحلة التالية.</p><button class="primary mt" onclick="${progress.completed.length===DATA.lessons.length?'startExam()':`toast('أكمل جميع الدروس أولًا')`}">${progress.examPassed?'إعادة الاختبار':'بدء الاختبار'}</button></div>`:academyFilterV3!=='all'?roadmapV3(academyFilterV3):''}`;
}

function openLabMenuV3(){
 modal(`<h2>خيارات المعمل</h2><p class="small">أدوات سريعة لمشروع Counter.</p><button class="primary" onclick="closeModal();checkLab()">✓ فحص المشروع</button><button class="secondary mt" onclick="closeModal();setLabTabV3('output')">عرض Output</button><button class="secondary danger mt" onclick="closeModal();resetLab()">↺ إعادة المشروع</button>`)
}
window.openLabMenuV3=openLabMenuV3;

function setLabTabV3(tab){labTabV3=tab;applyLabTabV3()}
window.setLabTabV3=setLabTabV3;

function scriptTextV3(){
 const connected=lab.signal;
 return `extends Control\n\nvar count = ${Number(lab.count)||0}\n\nfunc _ready():\n    $Label.text = str(count)\n\nfunc _on_button_pressed():\n    count += 1\n    $Label.text = str(count)\n\n# Signal: ${connected?'Button.pressed متصل ✓':'غير متصل — استخدم زر ربط Signal'}`
}
function ensureScriptPaneV3(){
 const r=document.getElementById('editorRoot');if(!r)return null;
 let p=r.querySelector('.scriptPaneV3');
 if(!p){p=document.createElement('div');p.className='scriptPaneV3';r.insertBefore(p,r.firstChild)}
 p.innerHTML=`<div class="scriptHeadV3"><span>res://counter.gd</span><b>GDScript</b></div><pre>${escapeHtmlV3(scriptTextV3())}</pre><div class="scriptTipV3">هذا عرض تعليمي للكود الذي يمثله مشروعك في المعمل. في مرحلة GDScript ستعدّله بنفسك.</div>`;
 return p
}
function applyLabTabV3(){
 const shell=document.querySelector('.labShell'),root=document.getElementById('editorRoot');if(!shell||!root)return;
 shell.dataset.labtab=labTabV3;
 document.querySelectorAll('.labTab[data-tab]').forEach(b=>b.classList.toggle('active',b.dataset.tab===labTabV3));
 const ws=root.querySelector('.editorWorkspace'),consoleEl=root.querySelector('.console'),scriptPane=ensureScriptPaneV3();
 if(!ws)return;
 const scene=ws.querySelector('.sceneCol'),ins=ws.querySelector('.inspectorCol'),view=ws.querySelector('.viewportPreview'),fs=ws.querySelector('.fileSystem');
 [scene,ins,view,fs].forEach(x=>{if(x)x.style.display=''});
 ws.style.display='grid'; if(scriptPane)scriptPane.style.display='none'; if(consoleEl)consoleEl.style.display='block';
 if(labTabV3==='preview'){
   if(scene)scene.style.display='none';if(ins)ins.style.display='none';if(fs)fs.style.display='none';if(consoleEl)consoleEl.style.display='none';if(view){view.style.display='grid';view.style.minHeight='330px'}
 }else if(labTabV3==='script'){
   ws.style.display='none';if(consoleEl)consoleEl.style.display='none';if(scriptPane)scriptPane.style.display='block'
 }else if(labTabV3==='output'){
   ws.style.display='none';if(scriptPane)scriptPane.style.display='none';if(consoleEl){consoleEl.style.display='block';consoleEl.style.minHeight='260px'}
 }else {
   if(view)view.style.minHeight='150px';if(consoleEl)consoleEl.style.minHeight='58px'
 }
}
window.applyLabTabV3=applyLabTabV3;

const renderLabBeforeV3=renderLab;
renderLab=function(){renderLabBeforeV3();applyLabTabV3()};

labPage=function(s){
 s.innerHTML=`<section class="hero labHero"><span class="kicker">المعمل التفاعلي</span><h1>مشروع Counter</h1><p>جرّب نسخة مصغرة من بيئة Godot: Scene Tree + Inspector + Preview + Script + Output.</p></section>
 <div class="sectionTitle"><h2>المحرر</h2><span>${progress.labPassed?'مكتمل ✓':'تدريب عملي'}</span></div>
 <div class="labShell"><div class="editorTopbar"><button class="runSquare" onclick="runLab()">▶</button><button class="stopSquare" onclick="lab.console='Stopped.';renderLab()">■</button><button class="moreSquare" onclick="openLabMenuV3()">•••</button><div class="editorTitle">Counter — Godot Mini Lab</div></div>
 <div class="labTabs"><button class="labTab" data-tab="scene" onclick="setLabTabV3('scene')">المشهد</button><button class="labTab" data-tab="preview" onclick="setLabTabV3('preview')">مشاهدة</button><button class="labTab" data-tab="script" onclick="setLabTabV3('script')">Script</button><button class="labTab" data-tab="output" onclick="setLabTabV3('output')">Output</button></div>
 <div class="nodePalette">${['Label','Button','LineEdit','TextureRect','ColorRect'].map(n=>`<button class="nodeChip" onclick="addLabNode('${n}')">+ ${n}</button>`).join('')}</div><div id="editorRoot"></div>
 <div class="labActions"><button class="secondary" onclick="connectSignal()">ربط Signal</button><button class="primary" onclick="runLab();setLabTabV3('preview')">▶ تشغيل</button><button class="secondary" onclick="resetLab()">↺ إعادة</button><button class="secondary" onclick="checkLab()">✓ فحص</button></div><div class="labHint">المهمة: Main(Control) يحتوي Label وButton، نص Label رقم، نص Button هو +1، ثم اربط pressed وشغّل.</div></div>`;
 renderLab()
};

/* Make native page changes reapply interaction state. */
const goBeforeV3=go;
go=function(p){if(p!=='academy'){academySearchV3='';academySearchOpenV3=false}goBeforeV3(p)};

})();