/* Godot Academy v6 — visual lesson identity + responsive diagrams */
(function(){
  const $=(s,r=document)=>r.querySelector(s);
  const $$=(s,r=document)=>[...r.querySelectorAll(s)];
  const escV6=(s)=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

  function svgIcon(name){
    const common='viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"';
    const icons={
      home:'<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10.5V21h13V10.5M9.5 21v-6h5v6"/>',
      book:'<path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22z"/><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22z"/>',
      lab:'<path d="M9 3h6M10 3v6l-5.5 9.5A1.7 1.7 0 0 0 6 21h12a1.7 1.7 0 0 0 1.5-2.5L14 9V3"/><path d="M7.5 16h9"/>',
      chart:'<path d="M5 20V10M12 20V4M19 20v-7"/>',
      compass:'<circle cx="12" cy="12" r="9"/><path d="m15.5 8.5-2 5-5 2 2-5z"/>',
      node:'<circle cx="12" cy="5" r="2.3"/><circle cx="5" cy="18" r="2.3"/><circle cx="19" cy="18" r="2.3"/><path d="M12 7.5V11M12 11 5.8 15.8M12 11l6.2 4.8"/>',
      inspector:'<path d="M4 5h16M4 12h16M4 19h16"/><circle cx="9" cy="5" r="2" fill="currentColor" stroke="none"/><circle cx="15" cy="12" r="2" fill="currentColor" stroke="none"/><circle cx="8" cy="19" r="2" fill="currentColor" stroke="none"/>',
      code:'<path d="m8.5 8-4 4 4 4M15.5 8l4 4-4 4M13.5 5l-3 14"/>',
      play:'<path d="M8 5v14l11-7z"/>',
      signal:'<circle cx="5" cy="12" r="2"/><circle cx="19" cy="12" r="2"/><path d="M7 12h10M13 8l4 4-4 4"/>',
      phone:'<rect x="6.5" y="2.5" width="11" height="19" rx="2"/><path d="M10 18.5h4"/>',
      image:'<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8" cy="9" r="1.5"/><path d="m4 17 5-5 3 3 2-2 6 5"/>',
      idea:'<path d="M9 18h6M10 21h4"/><path d="M8.2 15.5A7 7 0 1 1 15.8 15.5c-.8.6-1.2 1.2-1.3 2.5h-5c-.1-1.3-.5-1.9-1.3-2.5z"/>',
      why:'<circle cx="12" cy="12" r="9"/><path d="M9.8 9a2.4 2.4 0 1 1 3.7 2c-1 .7-1.5 1.1-1.5 2.5M12 17h.01"/>',
      how:'<path d="M4 17 17 4l3 3L7 20H4zM14 7l3 3"/>',
      example:'<path d="M5 4h14v16H5z"/><path d="M8 8h8M8 12h8M8 16h5"/>',
      tip:'<path d="M9 18h6M10 21h4"/><path d="M8.3 15.3a6.5 6.5 0 1 1 7.4 0c-.7.6-1.1 1.3-1.2 2.2h-5c-.1-.9-.5-1.6-1.2-2.2z"/>',
      warn:'<path d="M12 3 2.8 20h18.4z"/><path d="M12 9v4M12 17h.01"/>',
      check:'<circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/>',
      layers:'<path d="m12 3 9 5-9 5-9-5z"/><path d="m3 12 9 5 9-5M3 16l9 5 9-5"/>',
      export:'<path d="M12 3v12M8 7l4-4 4 4"/><path d="M5 13v7h14v-7"/>',
      game:'<path d="M7.5 9h9a4.5 4.5 0 0 1 4.1 6.3l-1.2 2.8a2 2 0 0 1-3.4.5L14.7 17H9.3L8 18.6a2 2 0 0 1-3.4-.5l-1.2-2.8A4.5 4.5 0 0 1 7.5 9z"/><path d="M7 12v4M5 14h4M16 13h.01M18 15h.01"/>'
    };
    return `<svg ${common}>${icons[name]||icons.layers}</svg>`;
  }

  function stageIconName(stage){return ({1:'compass',2:'code',3:'phone',4:'game',5:'layers',6:'phone',7:'export',8:'check'})[stage]||'layers'}
  function lessonIconName(l){
    const v=(l.visual||'').toLowerCase();
    if(v.includes('inspector'))return'inspector'; if(v.includes('script')||v.includes('code'))return'code';
    if(v.includes('run')||v.includes('debug'))return'play'; if(v.includes('scene')||v.includes('node'))return'node';
    if(v.includes('android')||v.includes('mobile'))return'phone'; if(v.includes('signal'))return'signal';
    return stageIconName(l.stageId||1);
  }

  function section(title,kind,html,iconName){
    return `<section class="v6Section ${kind}"><div class="v6SectionHeader"><span class="v6SectionIcon">${svgIcon(iconName)}</span><span>${title}</span></div><div class="v6SectionBody">${html}</div></section>`;
  }

  function codeVisual(l){
    const code=String(l.code||'').trim()||String(l.body||'').replace(/<[^>]+>/g,' ').trim().slice(0,120);
    return `<div class="v6CodePanel"><div class="v6CodeBar"><i></i><i></i><i></i><span>GDScript</span></div><pre>${escV6(code)}</pre></div>`;
  }
  function editorVisual(){return `<div class="v6MockEditor"><div class="v6MockScene"><b>SCENE</b><div class="v6MockTreeRow"><span class="v6Dot"></span>Main</div><div class="v6MockTreeRow child"><span class="v6Dot"></span>Label</div><div class="v6MockTreeRow child"><span class="v6Dot"></span>Button</div></div><div class="v6MockViewport"><div class="v6MockWindow"><div class="big">0</div><span class="btn">+1</span></div></div><div class="v6MockBottom"><div class="v6MockInspector"><b>INSPECTOR</b><div class="v6Prop"><span>Text</span><span>+1</span></div><div class="v6Prop"><span>Layout</span><span>Full Rect</span></div></div><div class="v6MockFiles">FILESYSTEM<br>res://<br> Main.tscn</div></div></div>`}
  function inspectorVisual(){return `<div class="v6InspectorMock"><div>Inspector</div><div class="v6InspectorRow"><label>Text</label><div class="v6InspectorValue">0</div></div><div class="v6InspectorRow"><label>Layout</label><div class="v6InspectorValue">Full Rect</div></div><div class="v6InspectorRow"><label>Theme</label><div class="v6InspectorValue">Default</div></div><div class="v6InspectorRow"><label>Position</label><div class="v6InspectorValue">x: 0  y: 0</div></div></div>`}
  function runVisual(){return `<div class="v6RunMock"><div class="v6RunToolbar"><span>▶ Run</span><span>Output</span><span>Debugger</span></div><div class="v6Terminal"><div class="ok">Running project...</div><div class="err">Node not found: "Lebel"</div><div>Check Scene Tree name → Label</div></div></div>`}
  function nodesVisual(l){
    const terms=(l.terms||[]).slice(0,6);return `<div class="v6NodeGrid">${terms.map((t,i)=>`<div class="v6NodeTile">${svgIcon(['node','inspector','code','signal','phone','image'][i%6])}<b>${escV6(t)}</b></div>`).join('')}</div>`;
  }
  function flowVisual(l){
    const t=(l.terms||[]).filter(Boolean),a=t[0]||'الفكرة',b=t[1]||'الأداة',c=t[2]||'النتيجة';
    return `<div class="v6Flow"><div class="v6FlowNode">${svgIcon(lessonIconName(l))}<span>${escV6(a)}</span><small>ابدأ من المفهوم</small></div><div class="v6Arrow">→</div><div class="v6FlowNode">${svgIcon('how')}<span>${escV6(b)}</span><small>طبّقه داخل Godot</small></div><div class="v6Arrow">→</div><div class="v6FlowNode">${svgIcon('check')}<span>${escV6(c)}</span><small>لاحظ النتيجة</small></div></div>`;
  }
  function sceneVisual(){return `<div class="v6Flow"><div class="v6FlowNode">${svgIcon('node')}<span>Main</span><small>Root / Control</small></div><div class="v6Arrow">→</div><div class="v6FlowNode">${svgIcon('layers')}<span>Children</span><small>Label + Button</small></div><div class="v6Arrow">→</div><div class="v6FlowNode">${svgIcon('check')}<span>Scene</span><small>شجرة منظمة</small></div></div>`}
  function signalVisual(){return `<div class="v6Flow"><div class="v6FlowNode">${svgIcon('play')}<span>Button</span><small>المستخدم يضغط</small></div><div class="v6Arrow">→</div><div class="v6FlowNode">${svgIcon('signal')}<span>pressed</span><small>Signal / حدث</small></div><div class="v6Arrow">→</div><div class="v6FlowNode">${svgIcon('code')}<span>Function</span><small>الكود ينفّذ</small></div></div>`}
  function visualHtml(l){
    const v=(l.visual||'').toLowerCase();
    let canvas;
    if(v==='editor')canvas=editorVisual();
    else if(v==='inspector')canvas=inspectorVisual();
    else if(v==='script'||v==='code')canvas=codeVisual(l);
    else if(v==='run')canvas=runVisual();
    else if(v==='scene')canvas=sceneVisual();
    else if(v==='signal')canvas=signalVisual();
    else if(v==='ui-nodes'||v==='chooser')canvas=nodesVisual(l);
    else if((l.stageId||1)>=2 && (l.code||'').trim())canvas=codeVisual(l);
    else canvas=flowVisual(l);
    return `<div class="v6Visual"><div class="v6VisualTop"><b>صورة الفكرة داخل Godot</b><span>انظر ثم اقرأ</span></div><div class="v6VisualCanvas">${canvas}</div><div class="v6VisualCaption">هذا الرسم جزء من الشرح: ركّز على العلاقة بين العناصر، وليس على حفظ شكل الصورة فقط.</div></div>`;
  }

  function rebuildBody(body){
    if(!body||body.dataset.v6==='1')return;body.dataset.v6='1';
    const nodes=[...body.childNodes].filter(n=>n.nodeType===1 || (n.nodeType===3&&n.textContent.trim()));
    body.innerHTML='';let plain=0;
    nodes.forEach(n=>{
      if(n.nodeType===3){const text=n.textContent.trim();if(!text)return;n=document.createElement('p');n.textContent=text;}
      if(n.matches?.('.codeCardV4,.v6CodePanel')){body.appendChild(n);return;}
      let title='تفصيل مهم',kind='v6Detail',icon='layers';
      if(n.classList?.contains('example')){title='مثال تطبيقي';kind='v6Example';icon='example'}
      else if(n.classList?.contains('warning')){title='خطأ شائع / انتبه';kind='v6Warning';icon='warn'}
      else if(n.classList?.contains('note')){title='معلومة تساعدك';kind='v6Tip';icon='tip'}
      else if(n.tagName==='P'){
        const order=[['ما هو؟','v6What','idea'],['لماذا نحتاجه؟','v6Why','why'],['كيف تستخدمه؟','v6How','how'],['تفصيل مهم','v6Detail','layers']];
        [title,kind,icon]=order[Math.min(plain,order.length-1)];plain++;
      }
      const wrap=document.createElement('div');wrap.innerHTML=section(title,kind,n.outerHTML||n.textContent,icon);body.appendChild(wrap.firstElementChild);
    });
  }

  function addLessonHead(page,l){
    if($('.v6LessonHead',page))return;
    const terms=(l.terms||[]).slice(0,3).join(' • ');
    const head=document.createElement('div');head.className='v6LessonHead';
    head.innerHTML=`<div class="v6LessonHeadTop"><span class="v6LessonGlyph">${svgIcon(lessonIconName(l))}</span><div><b>هدف هذا الدرس</b><span>افهم ${escV6(l.title)} بدل حفظ أسماء الأدوات.</span></div></div><div class="v6LessonGoal">بعد الدرس يجب أن تستطيع شرح: <b>${escV6(terms||l.title)}</b> وربطها بما تفعله داخل Godot.</div>`;
    const termsEl=$('.terms',page);(termsEl||page.querySelector('h1')).insertAdjacentElement('afterend',head);
  }
  function replaceVisual(page,l){
    const old=[...page.querySelectorAll('.visualCard')].find(x=>!x.closest('.lessonBody'));
    if(old)old.outerHTML=visualHtml(l);else{
      const body=$('.lessonBody',page);if(body)body.insertAdjacentHTML('beforebegin',visualHtml(l));
    }
  }
  function addUnderstand(page,l){
    if($('.v6Understand',page))return;
    const quiz=$('.quiz',page);if(!quiz)return;
    const box=document.createElement('div');box.className='v6Understand';
    box.innerHTML=`<div class="v6UnderstandHead"><span class="v6SectionIcon">${svgIcon('check')}</span><span>تأكد من فهمك قبل السؤال</span></div><p>لا تنتقل للحفظ. حاول أن تجيب بصوتك أو في ذهنك:</p><ul><li>ما وظيفة <b>${escV6(l.terms?.[0]||l.title)}</b> بكلماتك؟</li><li>أين ستراه أو تستخدمه داخل Godot؟</li><li>ماذا قد يحدث لو استخدمته بطريقة خاطئة؟</li></ul>`;
    quiz.insertAdjacentElement('beforebegin',box);
  }
  function enhanceLesson(i){
    const page=$('.lessonPage');const l=DATA?.lessons?.[i];if(!page||!l)return;
    page.classList.add('v6Lesson');
    addLessonHead(page,l);replaceVisual(page,l);rebuildBody($('.lessonBody',page));addUnderstand(page,l);
    const quiz=$('.quiz',page);if(quiz)quiz.classList.add('v6Quiz');
  }

  function polishNav(){
    const map=[['الرئيسية','home'],['الدروس','book'],['المعمل','lab'],['تقدمي','chart'],['ماذا أستخدم؟','compass']];
    $$('#bottomNav .navBtn').forEach(btn=>{
      const txt=btn.textContent.trim(),hit=map.find(x=>txt.includes(x[0]));if(!hit)return;
      const b=btn.querySelector('b');if(b&&!b.querySelector('svg'))b.innerHTML=`<span class="v6NavIcon">${svgIcon(hit[1])}</span>`;
    });
  }
  function polishCards(){
    $$('.stageIcon').forEach((el,i)=>{if(!el.querySelector('svg')){const title=el.parentElement?.textContent||'';let s=DATA.stages.find(x=>title.includes(x.title))?.id||i+1;el.innerHTML=svgIcon(stageIconName(s));}});
    $$('.dashIconV5').forEach((el,i)=>{if(!el.querySelector('svg'))el.innerHTML=svgIcon(i?'lab':stageIconName(1));});
  }
  function polishGlobal(){polishNav();polishCards()}

  const prevOpenLesson=openLesson;
  openLesson=function(i){prevOpenLesson(i);setTimeout(()=>{enhanceLesson(i);polishGlobal()},0)};

  if(typeof render==='function'){
    const prevRender=render;render=function(){const r=prevRender.apply(this,arguments);setTimeout(polishGlobal,0);return r};
  }
  if(typeof nav==='function'){
    const prevNav=nav;nav=function(){const r=prevNav.apply(this,arguments);setTimeout(polishNav,0);return r};
  }
  if(typeof labPage==='function'){
    const prevLab=labPage;labPage=function(){const r=prevLab.apply(this,arguments);setTimeout(polishGlobal,0);return r};
  }

  setTimeout(()=>{polishGlobal();if(current==='lesson'&&lessonIndex>=0)enhanceLesson(lessonIndex)},20);
})();
