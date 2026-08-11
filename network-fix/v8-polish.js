// Network Academy v8 — production polish: remove update notes, fix Arabic/English bidi, and refine UI copy
(function(){
  const LATIN_RE=/(\b\d{1,3}(?:\.\d{1,3}){3}(?:\/\d{1,2})?\b|[A-Za-z][A-Za-z0-9]*(?:[\-‐‑–—_\/.][A-Za-z0-9]+)*(?:\s+[A-Za-z][A-Za-z0-9]*(?:[\-‐‑–—_\/.][A-Za-z0-9]+)*)*)/g;
  const SKIP=new Set(['SCRIPT','STYLE','CODE','PRE','INPUT','TEXTAREA','SELECT','OPTION','BDI','BDF']);
  let queued=false;

  function cleanCopy(){
    document.querySelectorAll('.v7GuideIntro,.v7NoAuto').forEach(el=>el.remove());
    document.querySelectorAll('.guidedLab .guideBtns .primary').forEach(btn=>{
      if(/ضع الأجهزة المطلوبة|بدون حل المهمة|تهيئة/.test(btn.textContent||'')) btn.textContent='⚡ تهيئة نقطة البداية';
    });
    const replacements=new Map([
      ['اضبط IPv4 بنفسك — لا توجد قيم جاهزة','اضبط عنوان IPv4 للجهازين'],
      ['اضبط IPv4 بنفسك - لا توجد قيم جاهزة','اضبط عنوان IPv4 للجهازين'],
      ['اضغط «ضع الأجهزة المطلوبة فقط — بدون حل المهمة».','اضغط «تهيئة نقطة البداية».'],
      ['اضغط «ضع الأجهزة المطلوبة فقط - بدون حل المهمة».','اضغط «تهيئة نقطة البداية».'],
      ['ضع الأجهزة المطلوبة فقط — بدون حل المهمة','تهيئة نقطة البداية'],
      ['ضع الأجهزة المطلوبة فقط - بدون حل المهمة','تهيئة نقطة البداية'],
      ['🎯 لا توجد أسئلة مكررة هنا',''],
      ['لا توجد أسئلة مكررة هنا','']
    ]);
    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    const list=[];let n;while(n=walker.nextNode())list.push(n);
    list.forEach(node=>{
      let v=node.nodeValue||'';
      replacements.forEach((to,from)=>{if(v.includes(from))v=v.split(from).join(to)});
      if(v!==node.nodeValue)node.nodeValue=v;
    });
  }

  function shouldSkip(node){
    const p=node.parentElement;if(!p)return true;
    if(SKIP.has(p.tagName)||p.closest('.v8Ltr'))return true;
    if(p.isContentEditable)return true;
    return false;
  }

  function isolateNode(node){
    if(shouldSkip(node))return;
    const text=node.nodeValue||'';
    LATIN_RE.lastIndex=0;
    if(!LATIN_RE.test(text))return;
    LATIN_RE.lastIndex=0;
    const frag=document.createDocumentFragment();let last=0,m;
    while((m=LATIN_RE.exec(text))){
      if(m.index>last)frag.appendChild(document.createTextNode(text.slice(last,m.index)));
      const b=document.createElement('bdi');b.dir='ltr';b.className='v8Ltr';b.textContent=m[0];frag.appendChild(b);last=m.index+m[0].length;
      if(m[0].length===0)LATIN_RE.lastIndex++;
    }
    if(last<text.length)frag.appendChild(document.createTextNode(text.slice(last)));
    node.parentNode&&node.parentNode.replaceChild(frag,node);
  }

  function fixBidi(){
    const roots=document.querySelectorAll('.lessonPage,.guidedLab,.v7Foundation,.lessonSectionV4,.glossCard,.practiceCard,.labPanel,#modalRoot .modal,.statusBox,.guideStep');
    roots.forEach(root=>{
      root.setAttribute('dir','rtl');
      const w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);const nodes=[];let n;while(n=w.nextNode())nodes.push(n);nodes.forEach(isolateNode);
    });
  }

  function clarifyControls(){
    document.querySelectorAll('.themeBtn,.iconBtn').forEach(btn=>{
      const t=(btn.getAttribute('aria-label')||btn.title||'').toLowerCase();
      if(/theme|وضع|نهار|ليل/.test(t))btn.setAttribute('aria-label','تبديل الوضع النهاري والليلي');
    });
  }

  function run(){queued=false;cleanCopy();fixBidi();clarifyControls()}
  function queue(){if(queued)return;queued=true;setTimeout(run,0)}
  const obs=new MutationObserver(queue);obs.observe(document.documentElement,{childList:true,subtree:true,characterData:true});
  queue();
})();
