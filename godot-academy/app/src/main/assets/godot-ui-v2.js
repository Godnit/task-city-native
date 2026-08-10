/* Godot Academy — visual screen overrides. Keeps original learning/lab logic. */
(function(){
function logoSvg(){return `<svg class="godot-mini" viewBox="0 0 64 64" aria-hidden="true"><defs><linearGradient id="gm" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#22D3EE"/><stop offset=".48" stop-color="#2F80FF"/><stop offset="1" stop-color="#2449BE"/></linearGradient></defs><path d="M14 24c0-7 5-12 12-13l2-6 4 5 4-5 2 6c7 1 12 6 12 13v20c0 8-7 14-15 14H29c-8 0-15-6-15-14V24z" fill="url(#gm)"/><path d="M14 31 8 28v12l6 2M50 31l6-3v12l-6 2" fill="#2576E8"/><circle cx="25" cy="31" r="6" fill="#071425" stroke="#7DEEFF" stroke-width="2.5"/><circle cx="39" cy="31" r="6" fill="#071425" stroke="#7DEEFF" stroke-width="2.5"/><path d="M25 45c4 4 10 4 14 0" fill="none" stroke="#071425" stroke-width="2.8" stroke-linecap="round"/><circle cx="18" cy="10" r="3.5" fill="#5EDCFF"/><circle cx="32" cy="5" r="3.5" fill="#3E83FF"/><circle cx="46" cy="10" r="3.5" fill="#5EDCFF"/><path d="M20 12 27 17M32 9v8M44 12l-7 5" stroke="#367DFF" stroke-width="2"/></svg>`}
function graphSvg(){return `<svg viewBox="0 0 330 55" width="100%" height="100%"><g fill="none" stroke="#2D63C8" stroke-width="1.6" opacity=".78"><path d="M15 31 55 13l35 24 52-17 44 22 55-27 70 18"/><path d="M55 13 77 50M142 20l12 30M241 15l28 35"/></g><g fill="#2F80FF"><circle cx="15" cy="31" r="4"/><circle cx="55" cy="13" r="4"/><circle cx="90" cy="37" r="4"/><circle cx="142" cy="20" r="4"/><circle cx="186" cy="42" r="4"/><circle cx="241" cy="15" r="4"/><circle cx="311" cy="33" r="4"/></g><g fill="#22D3EE"><circle cx="77" cy="50" r="3"/><circle cx="154" cy="50" r="3"/><circle cx="269" cy="50" r="3"/></g></svg>`}

header=function(sub='تعلّم • ابنِ • انشر'){
 document.getElementById('topbar').innerHTML=`<div class="brand"><div class="brandLogoWrap">${logoSvg()}</div><div class="brandText"><div class="brandTitle"><em>Godot</em> أكاديمية</div><div class="brandSub">${sub}</div></div></div><button class="iconBtn" onclick="toggleTheme()">${settings.theme==='dark'?'☼':'☾'}</button>`
}
nav=function(){
 const items=[['home','⌂','الرئيسية'],['academy','▤','الدروس'],['lab','⌬','المعمل'],['progress','⌁','تقدمي']];
 document.getElementById('bottomNav').style.gridTemplateColumns='repeat(4,1fr)';
 document.getElementById('bottomNav').innerHTML=items.map(x=>`<button class="navBtn ${current===x[0]?'active':''}" onclick="go('${x[0]}')"><b>${x[1]}</b>${x[2]}</button>`).join('')
}

home=function(s){
 const n=nextLesson(),p=pct();
 s.innerHTML=`
 <section class="hero godotHero"><div class="nodeGraph">${graphSvg()}</div><div class="welcome">مرحباً 👋</div><h1>جاهز لتطوير مشروعك القادم؟</h1><p>تعلّم Godot خطوة بخطوة، وافهم العقد والإشارات والبرمجة، ثم ابنِ تطبيقات وألعابًا حقيقية.</p><button class="primary heroCTA" onclick="${n>=0?`openLesson(${n})`:`go('lab')`}">${n>=0?'ابدأ التعلم':'افتح المعمل'} ↗</button></section>
 <div class="sectionTitle"><h2>مسارك الحالي</h2><span>أساسيات Godot</span></div>
 <div class="card currentPathCard" onclick="go('academy')"><div class="pathThumb">◈</div><div class="pathInfo"><h3>أساسيات Godot</h3><p>${progress.completed.length} من ${DATA.lessons.length} درس مكتمل</p><div class="progressBar"><div class="progressFill" style="width:${p}%"></div></div></div><div class="circleProgress" style="--p:${p}%"><b>${p}%</b></div></div>
 <div class="sectionTitle"><h2>استكمل من حيث توقفت</h2><span>${n>=0?'درس جديد':'المعمل'}</span></div>
 <div class="card continueCard"><div class="continueArt"></div><div class="continueText"><h3>${n>=0?DATA.lessons[n].title:'مشروع Counter'}</h3><p>${n>=0?DATA.lessons[n].brief:'طبّق Scene + Label + Button + Signal.'}</p><button class="miniContinue" onclick="${n>=0?`openLesson(${n})`:`go('lab')`}">متابعة</button></div></div>
 <div class="sectionTitle"><h2>أدوات سريعة</h2><span>تعلّم حسب حاجتك</span></div>
 <div class="toolGrid"><button class="toolCard" onclick="go('helper')"><b>⌘ ماذا أستخدم؟</b><span>اختر هدفك وسنقترح الـNode المناسبة.</span></button><button class="toolCard" onclick="go('lab')"><b>▶ المعمل التفاعلي</b><span>جرّب Scene Tree وInspector وتشغيل المشروع.</span></button></div>
 <div class="sectionTitle"><h2>المسار الكامل</h2><span>8 مراحل</span></div>
 ${DATA.stages.map((st,i)=>`<div class="card stageCard ${i>0?'locked':''}" onclick="${i===0?`go('academy')`:`toast('هذه المرحلة ستُبنى في المراحل القادمة')`}"><div class="stageIcon">${i===0?'◉':i===1?'{}':i===2?'▣':i===3?'🎮':st.icon}</div><div class="stageBody"><h3>${st.title}</h3><p>${st.desc}</p></div><span class="badge">${i===0?p+'%':'🔒'}</span></div>`).join('')}`
}

academy=function(s){
 const p=pct();
 s.innerHTML=`
 <div class="lessonTop"><h1>الدروس</h1><button class="iconBtn" onclick="toast('البحث سيضاف مع توسع المحتوى')">⌕</button></div>
 <div class="filterRow"><button class="filterChip active">الكل</button><button class="filterChip">الأساسيات</button><button class="filterChip">البرمجة</button><button class="filterChip">2D</button><button class="filterChip">3D</button></div>
 <div class="sectionTitle"><h2>المسارات التعليمية</h2><span>تدرج واضح</span></div>
 <div class="card trackCard"><div class="trackBadge">${logoSvg()}</div><div><h3>أساسيات Godot</h3><p>الواجهة، العقد، المشاهد، Inspector، Signals وأول تطبيق.</p></div><div class="trackProgress" style="--tp:${p}%"><span>${p}%</span></div></div>
 <div class="card trackCard locked"><div class="trackBadge">{ }</div><div><h3>البرمجة بـ GDScript</h3><p>المتغيرات، الشروط، الدوال والمنطق العملي.</p></div><div class="trackProgress" style="--tp:0%"><span>0%</span></div></div>
 <div class="card trackCard locked"><div class="trackBadge">🎮</div><div><h3>تطوير ألعاب 2D</h3><p>حركة وتصادم وكاميرا ومشاريع كاملة.</p></div><div class="trackProgress" style="--tp:0%"><span>0%</span></div></div>
 <div class="card trackCard locked"><div class="trackBadge">◆</div><div><h3>تطوير ألعاب 3D</h3><p>العالم ثلاثي الأبعاد والفيزياء والإضاءة.</p></div><div class="trackProgress" style="--tp:0%"><span>0%</span></div></div>
 <div class="sectionTitle"><h2>دروس الأساسيات</h2><span>${progress.completed.length}/${DATA.lessons.length}</span></div>
 ${DATA.lessons.map((l,i)=>{let u=unlocked(i),d=progress.completed.includes(i);return `<div class="card lessonCard ${u?'':'locked'}" onclick="${u?`openLesson(${i})`:`toast('أكمل الدرس السابق أولًا')`}"><div class="lessonNum">${d?'✓':String(i+1).padStart(2,'0')}</div><div class="lessonInfo"><h3>${l.title}</h3><p>${l.brief}</p></div><span class="badge">${d?'100%':u?'ابدأ':'🔒'}</span></div>`}).join('')}
 <div class="sectionTitle"><h2>اختبار المرحلة</h2><span>أسئلة جديدة</span></div><div class="card ${progress.completed.length===DATA.lessons.length?'':'locked'}"><b>اختبار فهم مستقل</b><p class="small">لا يكرر أسئلة نهاية الدروس، ويفتح جاهزية المرحلة التالية.</p><button class="primary mt" onclick="${progress.completed.length===DATA.lessons.length?'startExam()':`toast('أكمل جميع الدروس أولًا')`}">${progress.examPassed?'إعادة الاختبار':'بدء الاختبار'}</button></div>`
}

labPage=function(s){
 s.innerHTML=`<section class="hero labHero"><span class="kicker">المعمل التفاعلي</span><h1>مشروع Counter</h1><p>جرّب نسخة مصغرة من بيئة Godot: Scene Tree + Inspector + Preview + FileSystem.</p></section>
 <div class="sectionTitle"><h2>المحرر</h2><span>${progress.labPassed?'مكتمل ✓':'تدريب عملي'}</span></div>
 <div class="labShell"><div class="editorTopbar"><button class="runSquare" onclick="runLab()">▶</button><button class="stopSquare" onclick="lab.console='Stopped.';renderLab()">■</button><button class="moreSquare">•••</button><div class="editorTitle">Counter — Godot Mini Lab</div></div><div class="labTabs"><button class="labTab active">المشهد</button><button class="labTab">مشاهدة</button><button class="labTab">Script</button><button class="labTab">Output</button></div><div class="nodePalette">${['Label','Button','LineEdit','TextureRect','ColorRect'].map(n=>`<button class="nodeChip" onclick="addLabNode('${n}')">+ ${n}</button>`).join('')}</div><div id="editorRoot"></div><div class="labActions"><button class="secondary" onclick="connectSignal()">ربط Signal</button><button class="primary" onclick="runLab()">▶ تشغيل</button><button class="secondary" onclick="resetLab()">↺ إعادة</button><button class="secondary" onclick="checkLab()">✓ فحص</button></div><div class="labHint">المهمة: Main(Control) يحتوي Label وButton، نص Label رقم، نص Button هو +1، ثم اربط pressed وشغّل.</div></div>`;
 renderLab()
}

renderLab=function(){
 const root=document.getElementById('editorRoot');if(!root)return;
 const sel=lab.nodes.find(n=>n.id===lab.selected)||lab.nodes[0];
 const isRoot=sel.id==='root';
 root.innerHTML=`<div class="editorWorkspace">
 <div class="sceneCol"><div class="panelHead"><span>شجرة المشهد</span><b>＋</b></div><div class="sceneTree">${lab.nodes.map(n=>`<button class="treeNode ${n.id===lab.selected?'selected':''}" onclick="selectLab('${n.id}')">${n.id==='root'?'Main':'└ '+n.name}</button>`).join('')}</div></div>
 <div class="inspectorCol"><div class="panelHead"><span>Inspector</span><span>⌕</span></div><div class="inspectorBody"><h4>${sel.type||'Control'}</h4>${isRoot?`<div class="inspectorGroup"><div class="propRow"><label>Layout</label><input value="Full Rect" disabled></div><div class="propRow"><label>Anchors</label><input value="Preset" disabled></div><div class="propRow"><label>Size</label><input value="360 × 640" disabled></div><div class="propRow"><label>Visibility</label><input value="Visible ✓" disabled></div></div>`:`<div class="propRow"><label>Node Name</label><input value="${esc(sel.name)}" oninput="updateLab('name',this.value)"></div>${['Label','Button','LineEdit'].includes(sel.type)?`<div class="propRow"><label>Text</label><input value="${esc(sel.text||'')}" oninput="updateLab('text',this.value)"></div>`:''}<div class="inspectorGroup"><div class="propRow"><label>Layout</label><input value="Top Left" disabled></div><div class="propRow"><label>Visible</label><input value="✓" disabled></div></div><button class="secondary danger" onclick="removeLab()">حذف Node</button>`}</div></div>
 <div class="viewportPreview"><span class="previewBadge">Preview / Viewport</span><div class="appPreview"><div class="pLabel" id="previewLabel">${getNodeText('Label')??'—'}</div><button class="pButton" onclick="previewPress()">${getNodeText('Button')??'Button'}</button></div></div>
 <div class="fileSystem"><div class="fileHead">FileSystem — res://</div><div class="fileRows"><span class="filePill">📁 scenes</span><span class="filePill">📁 scripts</span><span class="filePill">📁 assets</span><span class="filePill">◇ icon.svg</span></div></div></div><div class="console">${esc(lab.console)}</div>`
}

progressPage=function(s){
 const done=progress.completed.length, xp=done*90+(progress.labPassed?220:0)+(progress.examPassed?250:0),level=Math.max(1,Math.min(6,1+Math.floor(xp/250))),need=(level+1)*250,ready=done===DATA.lessons.length&&progress.labPassed&&progress.examPassed;
 const points=[12,20,15,24,18,31,27,40,34,50,47,63];
 s.innerHTML=`<div class="sectionTitle"><h2>تقدمي</h2><span>المستوى والمهارات</span></div>
 <div class="card levelHero"><div class="levelInfo"><small>مستواك الحالي</small><h2>مبرمج Godot</h2><div class="small">المستوى ${level}</div><div class="progressBar"><div class="progressFill" style="width:${Math.min(100,Math.round((xp%250)/250*100))}%"></div></div><div class="small">${xp} XP</div></div><div class="levelBadge"><div class="hex">${level}</div></div></div>
 <div class="sectionTitle"><h2>نظرة عامة</h2><span>${ready?'المرحلة مكتملة ✓':'واصل التقدم'}</span></div><div class="overviewGrid"><div class="overviewItem">▤<b>${done}</b><span>الدروس المكتملة</span></div><div class="overviewItem">⌘<b>${progress.examPassed?1:0}</b><span>الاختبارات المجتازة</span></div><div class="overviewItem">🎮<b>${progress.labPassed?1:0}</b><span>المشاريع المنجزة</span></div></div>
 <div class="sectionTitle"><h2>الشارات</h2><span>إنجازاتك</span></div><div class="badgeRow"><div class="skillBadge"><div class="skillIcon">◈</div><span>أول خطوة</span></div><div class="skillBadge"><div class="skillIcon">⌬</div><span>مستكشف العقد</span></div><div class="skillBadge"><div class="skillIcon">{ }</div><span>مبرمج GDScript</span></div><div class="skillBadge"><div class="skillIcon">🎮</div><span>مطور 2D</span></div></div>
 <div class="sectionTitle"><h2>النشاط الأسبوعي</h2><span>${done*20+60} دقيقة</span></div><div class="activity"><div class="chartLine"><svg viewBox="0 0 240 70" preserveAspectRatio="none"><polyline points="${points.map((v,i)=>`${i*21},${70-v}`).join(' ')}" fill="none" stroke="#2F80FF" stroke-width="3"/><g fill="#22D3EE">${points.map((v,i)=>`<circle cx="${i*21}" cy="${70-v}" r="2.4"/>`).join('')}</g></svg></div></div>
 <div class="sectionTitle"><h2>متطلبات المرحلة</h2><span>تقدم حقيقي</span></div><div class="card"><ul class="checklist"><li class="${done===DATA.lessons.length?'done':''}">الدروس: ${done}/${DATA.lessons.length}</li><li class="${progress.labPassed?'done':''}">مشروع Counter: ${progress.labPassed?'مكتمل ✓':'غير مكتمل'}</li><li class="${progress.examPassed?'done':''}">اختبار المرحلة: ${progress.examPassed?'ناجح ✓':'لم ينجح بعد'}</li></ul></div>`
}

const oldHelper=helperPage;
helperPage=function(s){oldHelper(s);const h=s.querySelector('.hero');if(h){h.classList.add('labHero');h.querySelector('h1').textContent='ماذا أستخدم في Godot؟'}}

// Refresh the current screen after overrides load.
header();nav();render();
})();
