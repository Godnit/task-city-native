/* Godot Academy v5 — responsive fixes, lesson→lab workflow, hierarchy lab, professional home */
(function(){
  const V5_RETURN_KEY='godot_academy_lab_return_v5';
  const V5_TREE_KEY='godot_academy_tree_lab_v5';
  let returnLesson=-1;
  try{returnLesson=Number(localStorage.getItem(V5_RETURN_KEY)||-1)}catch(e){}

  function idsV5(stage){return DATA.lessons.map((l,i)=>l.stageId===stage?i:-1).filter(i=>i>=0)}
  function lessonsDoneV5(stage){const a=idsV5(stage);return a.length>0&&a.every(i=>progress.completed.includes(i))}
  function examDoneV5(stage){return stage===1?!!progress.examPassed:!!progress.stageExams?.[stage]}
  function labDoneV5(stage){return stage===1?!!progress.labPassed:!!progress.stageLabs?.[stage]}
  function stageDoneV5(stage){return lessonsDoneV5(stage)&&examDoneV5(stage)&&labDoneV5(stage)}
  function stageUnlockedV5(stage){return stage===1||stageDoneV5(stage-1)}
  function stagePctV5(stage){const a=idsV5(stage);const lessons=a.length?a.filter(i=>progress.completed.includes(i)).length/a.length*70:0;return Math.round(lessons+(examDoneV5(stage)?15:0)+(labDoneV5(stage)?15:0))}
  function highestStageV5(){let h=1;for(let s=2;s<=8;s++)if(stageUnlockedV5(s))h=s;return h}
  function allPctV5(){let doneLessons=progress.completed.length,extras=0;for(let s=1;s<=8;s++){if(examDoneV5(s))extras++;if(labDoneV5(s))extras++}return Math.round((doneLessons+extras)/(DATA.lessons.length+16)*100)}
  function nextLessonV5(){for(let s=1;s<=8;s++){if(!stageUnlockedV5(s))break;for(const i of idsV5(s))if(!progress.completed.includes(i))return i}return -1}
  function selectedIcon(){return '<img src="godot_academy_icon_v5.jpg" alt="أكاديمية Godot">'}

  header=function(sub='تعلّم • طبّق • ابنِ'){
    const root=document.getElementById('topbar');
    if(!root)return;
    root.innerHTML=`<div class="brand"><div class="brandLogoWrapV5">${selectedIcon()}</div><div class="brandText"><div class="brandTitleV5"><em>Godot</em> أكاديمية</div><div class="brandSubV5">${sub}</div></div></div><button class="themeBtnV5" onclick="toggleTheme()" aria-label="تبديل الوضع">${settings.theme==='dark'?'☼':'☾'}</button>`;
  };

  home=function(s){
    const next=nextLessonV5(),h=highestStageV5(),all=allPctV5();
    const currentStage=DATA.stages[h-1];
    const stages=DATA.stages.map(st=>`<button class="stageRoadItemV5 ${stageUnlockedV5(st.id)?'':'locked'}" onclick="${stageUnlockedV5(st.id)?`openStageV4(${st.id})`:`toast('أكمل المرحلة السابقة: الدروس + الاختبار + المشروع')`}"><span class="stageRoadNumV5">${st.id}</span><span class="stageRoadBodyV5"><b>${st.icon} ${st.title}</b><span>${st.desc}</span></span><span class="stageRoadPctV5">${stageUnlockedV5(st.id)?stagePctV5(st.id)+'%':'🔒'}</span></button>`).join('');
    s.innerHTML=`
      <section class="homeHeroV5">
        <div class="homeHeroTopV5">
          <div class="homeHeroTextV5"><span class="eyebrow">◈ أكاديمية عملية</span><h1>تعلّم Godot كما تستخدمه فعلاً.</h1><p>من Scene وNode إلى GDScript و2D و3D والهاتف والتصدير. اقرأ الفكرة ثم انتقل للمعمل وطبّقها بنفسك.</p></div>
          <img class="heroMascotV5" src="godot_academy_icon_v5.jpg" alt="شعار أكاديمية Godot">
        </div>
        <div class="homeHeroActionsV5"><button class="primary" onclick="${next>=0?`openLesson(${next})`:`openLabStageV4(8)`}">${next>=0?'واصل من حيث توقفت':'افتح مشروع التخرج'} ←</button><button class="ghostBtnV5" onclick="go('lab')">المعمل</button></div>
        <div class="homeStatsV5"><div class="homeStatV5"><b>${all}%</b><span>التقدم الكلي</span></div><div class="homeStatV5"><b>${progress.completed.length}</b><span>من 97 درسًا</span></div><div class="homeStatV5"><b>${h}/8</b><span>المرحلة المتاحة</span></div></div>
      </section>
      <div class="sectionTitle"><h2>لوحة العمل</h2><span>تعلم + تطبيق</span></div>
      <div class="dashboardGridV5">
        <button class="dashCardV5" onclick="openStageV4(${h})"><div class="dashIconV5">${currentStage.icon}</div><h3>${currentStage.title}</h3><p>المرحلة الحالية • ${stagePctV5(h)}% مكتمل</p><div class="progressBar"><div class="progressFill" style="width:${stagePctV5(h)}%"></div></div></button>
        <button class="dashCardV5" onclick="go('lab')"><div class="dashIconV5">⌬</div><h3>Godot Mini Lab</h3><p>Scene Tree + Inspector + Script + تشغيل.</p><div class="miniSceneV5"></div></button>
      </div>
      <div class="sectionTitle"><h2>المسار الكامل</h2><span>8 مراحل • 97 درسًا</span></div>
      <div class="stageRoadV5">${stages}</div>
      <div class="sectionTitle"><h2>أدوات سريعة</h2><span>حسب حاجتك</span></div>
      <div class="toolGrid"><button class="toolCard" onclick="go('helper')"><b>⌘ ماذا أستخدم؟</b><span>اعرف الـNode أو الأداة المناسبة قبل أن تبدأ.</span></button><button class="toolCard" onclick="go('progress')"><b>⌁ تقدمي</b><span>الدروس والاختبارات والمشاريع ونقاط الإتقان.</span></button></div>`;
  };

  const oldOpenLessonV5=openLesson;
  openLesson=function(i){oldOpenLessonV5(i);window.setTimeout(()=>decorateLessonV5(i),0)};
  function decorateLessonV5(i){
    const page=document.querySelector('.lessonPage');if(!page)return;
    page.querySelector('.applyLabCardV5')?.remove();
    const quiz=page.querySelector('.quiz');if(!quiz)return;
    const l=DATA.lessons[i],card=document.createElement('div');card.className='applyLabCardV5';
    card.innerHTML=`<div class="applyLabIconV5">⌬</div><div><h3>طبّق هذا الدرس في المعمل</h3><p>افتح مشروع المرحلة وجرّب بنفسك مفاهيم <b>${l.terms.slice(0,3).join(' • ')}</b>. سيظهر زر للعودة إلى هذا الدرس مباشرة.</p><button class="primary" onclick="openLessonLabV5(${i})">فتح المعمل لهذا الدرس ←</button></div>`;
    page.insertBefore(card,quiz);
  }

  window.openLessonLabV5=function(i){returnLesson=i;try{localStorage.setItem(V5_RETURN_KEY,String(i))}catch(e){};window.openLabStageV4(DATA.lessons[i].stageId)};
  window.returnToLessonV5=function(){if(returnLesson<0||!DATA.lessons[returnLesson]){go('academy');return}openLesson(returnLesson)};

  const oldLabPageV5=labPage;
  labPage=function(screen){oldLabPageV5(screen);const active=[...document.querySelectorAll('.labStageTabsV4 .filterChip')].find(x=>x.classList.contains('active'));decorateLabV5(active?parseInt(active.textContent,10)||1:1)};
  const oldOpenLabStageV5=window.openLabStageV4;
  window.openLabStageV4=function(stage){oldOpenLabStageV5(stage);decorateLabV5(stage)};

  function addReturnBarV5(stage){
    const hub=document.getElementById('labHubV4');if(!hub)return;hub.querySelector('.labReturnV5')?.remove();
    if(returnLesson<0||DATA.lessons[returnLesson]?.stageId!==stage)return;
    const l=DATA.lessons[returnLesson];hub.insertAdjacentHTML('afterbegin',`<div class="labReturnV5"><span>أنت تطبق الآن درس: <b>${l.title}</b></span><button onclick="returnToLessonV5()">↩ رجوع للدرس</button></div>`);
  }
  function addLabContextV5(stage){
    const project=document.getElementById('labProjectV4');if(!project)return;project.querySelector('.v5LabContext')?.remove();
    const lesson=returnLesson>=0&&DATA.lessons[returnLesson]?.stageId===stage?DATA.lessons[returnLesson]:null,box=document.createElement('div');box.className='v5LabContext';
    box.innerHTML=lesson?`<b>مهمة مرتبطة بالدرس</b><p>ركّز أثناء التطبيق على: ${lesson.terms.join(' • ')}. لا تنتقل للنتيجة فقط؛ لاحظ أين توجد الأداة وما علاقتها ببقية العقد.</p>`:`<b>تدريب المرحلة ${stage}</b><p>نفّذ المشروع كأنك داخل Godot: اختر العناصر، عدّل الخصائص، شغّل، اقرأ النتيجة ثم أصلح الخطأ.</p>`;
    project.insertBefore(box,project.firstChild);
  }
  function decorateLabV5(stage){addReturnBarV5(stage);if(stage===1)renderHierarchyLabV5();else addLabContextV5(stage)}

  let treeState={nodes:[{id:'root',type:'Control',name:'Main',parent:null,text:''}],selected:'root',signal:false,running:false,count:0,console:'Select Main (Control), then add Label and Button as children.'};
  try{treeState={...treeState,...JSON.parse(localStorage.getItem(V5_TREE_KEY)||'{}')}}catch(e){}
  function saveTreeV5(){try{localStorage.setItem(V5_TREE_KEY,JSON.stringify(treeState))}catch(e){}}
  function nodeV5(id){return treeState.nodes.find(n=>n.id===id)}
  function firstTypeV5(type){return treeState.nodes.find(n=>n.type===type)}
  function isContainerV5(type){return ['Control','VBoxContainer','HBoxContainer','PanelContainer','MarginContainer'].includes(type)}
  function depthV5(n){let d=0,p=n;while(p&&p.parent){d++;p=nodeV5(p.parent)}return d}
  function childrenOfV5(parent){return treeState.nodes.filter(n=>n.parent===parent)}
  function orderedNodesV5(){const out=[];function walk(id){const n=nodeV5(id);if(!n)return;out.push(n);childrenOfV5(id).forEach(c=>walk(c.id))}walk('root');return out}
  function stepStatusV5(){const label=firstTypeV5('Label'),button=firstTypeV5('Button');return [true,!!label&&label.parent==='root',!!button&&button.parent==='root',!!label&&/^\d+$/.test(label.text||''),!!button&&button.text==='+1',treeState.signal,treeState.running]}
  function guideV5(){const s=stepStatusV5(),steps=['ابدأ من Main من نوع Control؛ هذه هي العقدة الأب للواجهة.','حدد Main ثم أضف Label. يجب أن يظهر متداخلًا تحته في Scene Tree.','حدد Main مرة أخرى ثم أضف Button كابن ثانٍ لـ Control.','من Inspector اجعل Text للـ Label رقمًا مثل 0.','حدد Button واجعل Text يساوي +1.','اربط Button.pressed بالدالة التي تزيد العداد.','شغّل Scene ثم اضغط الزر في Preview وشاهد Output.'];return `<div class="v5LabGuide">${steps.map((t,i)=>`<div class="v5GuideStep ${s[i]?'done':''}"><b>${s[i]?'✓':i+1}</b><span>${t}</span></div>`).join('')}</div>`}

  function renderHierarchyLabV5(){
    const root=document.getElementById('labProjectV4');if(!root)return;
    const sel=nodeV5(treeState.selected)||nodeV5('root'),label=firstTypeV5('Label'),button=firstTypeV5('Button');
    root.innerHTML=`<div class="v5LabContext"><b>لماذا نهتم بالشجرة؟</b><p>في Godot ليست العقد عناصر منفصلة فقط. العلاقة الأب/الابن مهمة: هنا <b>Button وLabel يجب أن يكونا داخل Main(Control)</b>. إذا حددت عقدة لا تستطيع احتواء أبناء، سيمنعك المعمل ويشرح السبب.</p></div>${guideV5()}<div class="v5Editor"><div class="v5EditorBar"><button class="v5Run" onclick="v5RunCounter()">▶</button><div class="v5EditorTitle">Counter.tscn — Godot Mini Lab</div></div><div class="v5HierarchyHint">حدد <strong>العقدة الأب</strong> أولًا ثم أضف العقدة الجديدة. التحديد الحالي: <strong>${sel.name} (${sel.type})</strong></div><div class="v5Palette">${['Label','Button','LineEdit','VBoxContainer','PanelContainer'].map(t=>`<button onclick="v5AddNode('${t}')">+ ${t}</button>`).join('')}</div><div class="v5Workspace"><div class="v5Scene"><div class="v5PanelHead"><span>Scene Tree</span><span>Parent → Child</span></div><div class="v5Tree">${orderedNodesV5().map(n=>`<button class="v5TreeNode ${n.id===sel.id?'active':''}" data-type="${n.type}" onclick="v5SelectNode('${n.id}')"><span class="v5TreeIndent" style="width:${depthV5(n)*14}px"></span><span class="v5TypeDot"></span><span class="v5NodeName">${depthV5(n)?'└ ':''}${n.name}</span></button>`).join('')}</div></div><div class="v5Inspector"><div class="v5PanelHead"><span>Inspector</span><span>${sel.type}</span></div><div class="v5InspectorBody"><span class="v5ParentPill">Parent: ${sel.parent?`${nodeV5(sel.parent)?.name} (${nodeV5(sel.parent)?.type})`:'لا يوجد — Root'}</span>${sel.id==='root'?`<div class="propRow"><label>Type</label><input value="Control" disabled></div><div class="propRow"><label>Layout</label><input value="Full Rect" disabled></div><p class="small">Control يستطيع احتواء عناصر الواجهة مثل Button وLabel.</p>`:`<div class="propRow"><label>Name</label><input value="${esc(sel.name)}" oninput="v5UpdateNode('name',this.value)"></div>${['Label','Button','LineEdit'].includes(sel.type)?`<div class="propRow"><label>Text</label><input value="${esc(sel.text||'')}" oninput="v5UpdateNode('text',this.value)"></div>`:''}<button class="secondary danger" onclick="v5RemoveNode()">حذف العقدة</button>`}</div></div><div class="v5Viewport"><div class="v5Phone"><div class="label">${label?esc(label.text||'0'):'—'}</div><button onclick="v5PreviewPress()">${button?esc(button.text||'Button'):'Button'}</button></div></div><div class="v5FileSystem"><span class="v5File">res://Counter.tscn</span><span class="v5File">res://counter.gd</span><span class="v5File">user://progress.cfg</span></div></div><div class="v5LabActions"><button class="secondary" onclick="v5ConnectSignal()">Signal</button><button class="primary" onclick="v5RunCounter()">▶ Run</button><button class="secondary" onclick="v5CheckCounter()">✓ فحص</button><button class="secondary" onclick="v5ResetCounter()">↺ إعادة</button></div><div class="v5Console">${esc(treeState.console)}</div></div>`;
  }

  window.v5SelectNode=function(id){treeState.selected=id;saveTreeV5();renderHierarchyLabV5()};
  window.v5AddNode=function(type){const parent=nodeV5(treeState.selected)||nodeV5('root');if(!isContainerV5(parent.type)){toast(`لا يمكن وضع ${type} داخل ${parent.type}. اختر Control أو Container كعقدة أب.`);return}if(['Label','Button'].includes(type)&&treeState.nodes.some(n=>n.type===type)){toast(`يوجد ${type} بالفعل في مشروع Counter`);return}const id='v5_'+Date.now().toString(36)+'_'+Math.random().toString(36).slice(2,5),text=type==='Label'?'0':type==='Button'?'+1':'';treeState.nodes.push({id,type,name:type,parent:parent.id,text});treeState.selected=id;treeState.console=`Added ${type} as child of ${parent.name} (${parent.type})`;saveTreeV5();renderHierarchyLabV5()};
  window.v5UpdateNode=function(key,value){const n=nodeV5(treeState.selected);if(!n||n.id==='root')return;n[key]=value;saveTreeV5();renderHierarchyLabV5()};
  window.v5RemoveNode=function(){const id=treeState.selected;if(id==='root')return;const remove=new Set([id]);let changed=true;while(changed){changed=false;treeState.nodes.forEach(n=>{if(n.parent&&remove.has(n.parent)&&!remove.has(n.id)){remove.add(n.id);changed=true}})}treeState.nodes=treeState.nodes.filter(n=>!remove.has(n.id));treeState.selected='root';treeState.signal=false;treeState.running=false;treeState.console='Node and its children removed.';saveTreeV5();renderHierarchyLabV5()};
  window.v5ConnectSignal=function(){const b=firstTypeV5('Button');if(!b){toast('أضف Button داخل Control أولًا');return}treeState.signal=true;treeState.console='Signal: Button.pressed -> _on_button_pressed()';saveTreeV5();renderHierarchyLabV5();toast('تم ربط pressed ✓')};
  window.v5RunCounter=function(){const l=firstTypeV5('Label'),b=firstTypeV5('Button');if(!l||!b){treeState.running=false;treeState.console='Run failed: Main(Control) must contain Label and Button.';saveTreeV5();renderHierarchyLabV5();toast('أكمل شجرة Scene أولًا');return}treeState.running=true;treeState.console='Running Counter.tscn...\nScene tree valid. Press +1 in Preview.';saveTreeV5();renderHierarchyLabV5();toast('المشهد يعمل ▶')};
  window.v5PreviewPress=function(){if(!treeState.running){toast('اضغط Run أولًا');return}if(!treeState.signal){treeState.console='Button pressed, but pressed Signal is not connected.';saveTreeV5();renderHierarchyLabV5();return}const l=firstTypeV5('Label');if(!l)return;treeState.count=Number(l.text)||0;treeState.count++;l.text=String(treeState.count);treeState.console=`Output: count = ${treeState.count}`;saveTreeV5();renderHierarchyLabV5()};
  window.v5CheckCounter=function(){const l=firstTypeV5('Label'),b=firstTypeV5('Button'),ok=!!l&&!!b&&l.parent==='root'&&b.parent==='root'&&/^\d+$/.test(l.text||'')&&b.text==='+1'&&treeState.signal&&treeState.running;if(ok){progress.labPassed=true;progress.stageLabs=progress.stageLabs||{};progress.stageLabs[1]=true;saveProgress();treeState.console='Challenge passed ✓ Main(Control) → Label + Button, text, signal and run are correct.';saveTreeV5();renderHierarchyLabV5();toast('نجح مشروع المرحلة الأولى ✓')}else toast('راجع قائمة الخطوات بالأعلى؛ كل خطوة يجب أن تتحول إلى ✓')};
  window.v5ResetCounter=function(){treeState={nodes:[{id:'root',type:'Control',name:'Main',parent:null,text:''}],selected:'root',signal:false,running:false,count:0,console:'Select Main (Control), then add Label and Button as children.'};saveTreeV5();renderHierarchyLabV5()};

  function openExamV5(stage){
    if(!lessonsDoneV5(stage)){toast('أكمل جميع دروس المرحلة أولًا');return}
    const isOne=stage===1,bank=isOne?exam:GODOT_EXAMS_V4[stage],pass=isOne?5:4;
    const rows=bank.map((q,i)=>{const question=isOne?q.q:q[0],opts=isOne?q.o:q[1];return `<div class="examQuestionV5"><strong>${i+1}. ${question}</strong><div class="examOptionsV5">${opts.map((o,k)=>`<label class="examOptionV5"><input type="radio" name="v5q${i}" value="${k}"><span>${o}</span></label>`).join('')}</div></div>`}).join('');
    modal(`<div class="examV5"><h2>اختبار المرحلة ${stage===1?'الأولى':stage}</h2><p class="small examIntroV5">${bank.length} أسئلة جديدة. تحتاج ${pass}/${bank.length} للنجاح.</p>${rows}<button class="primary" onclick="finishExamV5(${stage})">تصحيح الاختبار</button></div>`);
  }
  window.startExam=function(){openExamV5(1)};
  window.startExamV4b=function(stage){openExamV5(stage)};
  window.finishExamV5=function(stage){const isOne=stage===1,bank=isOne?exam:GODOT_EXAMS_V4[stage],pass=isOne?5:4;let score=0;bank.forEach((q,i)=>{const chosen=document.querySelector(`input[name="v5q${i}"]:checked`),ans=isOne?q.a:q[2];if(chosen&&Number(chosen.value)===ans)score++});if(score>=pass){if(isOne)progress.examPassed=true;else{progress.stageExams=progress.stageExams||{};progress.stageExams[stage]=true}saveProgress();closeModal();toast(`نجحت ${score}/${bank.length} ✓`);window.setTimeout(()=>openStageV4(stage),350)}else toast(`نتيجتك ${score}/${bank.length} — تحتاج ${pass}/${bank.length}`)};

  const oldProgressPageV5=progressPage;
  progressPage=function(s){oldProgressPageV5(s);s.innerHTML=s.innerHTML.replaceAll('96 درسًا','97 درسًا').replaceAll('96 درس','97 درس')};
  header();nav();render();
})();
