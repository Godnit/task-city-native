/* Electronics Academy v4 — mobile-first touch engine */
(function(){
'use strict';
const A=()=>window.EA_PHASE1;
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
let session=null;
function node(id){const a=A();return a&&a.E.nodes.find(n=>n.id===+id)}
function spec(n){const a=A();return n&&a&&a.EA_PARTS[n.type]}
function portWorld(n,pid){const d=spec(n),p=d&&d.ports.find(x=>x[0]===pid),side=p?p[3]:'right';return{x:n.x+(side==='right'?d.w/2:-d.w/2),y:n.y}}
function worldScale(){const w=document.getElementById('eaWorld');if(!w)return 1;const r=w.getBoundingClientRect();return r.width/1200||1}
function screenToWorld(x,y){const w=document.getElementById('eaWorld');if(!w)return{x:0,y:0};const r=w.getBoundingClientRect(),s=worldScale();return{x:(x-r.left)/s,y:(y-r.top)/s}}
function worldToScreen(p){const w=document.getElementById('eaWorld');if(!w)return{x:0,y:0};const r=w.getBoundingClientRect(),s=worldScale();return{x:r.left+p.x*s,y:r.top+p.y*s}}
function curve(a,b){const dx=Math.max(48,Math.abs(b.x-a.x)*.42),sg=b.x>=a.x?1:-1;return`M ${a.x} ${a.y} C ${a.x+dx*sg} ${a.y}, ${b.x-dx*sg} ${b.y}, ${b.x} ${b.y}`}
function wireSignal(w){const a=A(),n1=node(w.a),n2=node(w.b);if(!a||!n1||!n2)return'neutral';const p1=a.EA_PARTS[n1.type].ports.find(p=>p[0]===w.ap),p2=a.EA_PARTS[n2.type].ports.find(p=>p[0]===w.bp);return p1&&p1[2]!=='neutral'?p1[2]:p2&&p2[2]!=='neutral'?p2[2]:'neutral'}
function drawWires(temp){const a=A(),svg=document.getElementById('eaWires');if(!a||!svg)return;let h='';for(const w of a.E.wires){const n1=node(w.a),n2=node(w.b);if(!n1||!n2)continue;h+=`<path class="eaWire ${wireSignal(w)}" d="${curve(portWorld(n1,w.ap),portWorld(n2,w.bp))}"/>`}
 if(temp)h+=`<path class="eaWire ${temp.cls||'neutral'} eaTempWireV4" d="${curve(temp.a,temp.b)}"/>`;
 svg.innerHTML=h;
}
function closestPort(x,y,src){const a=A();if(!a)return null;let best=null,bestD=1e9;for(const n of a.E.nodes){if(src&&n.id===src.node)continue;for(const p of a.EA_PARTS[n.type].ports){const q=worldToScreen(portWorld(n,p[0])),d=Math.hypot(x-q.x,y-q.y);if(d<bestD){bestD=d;best={node:n.id,port:p[0],distance:d}}}}return bestD<=74?best:null}
function clearTargets(){document.querySelectorAll('.terminal.eaDropTargetV4').forEach(x=>x.classList.remove('eaDropTargetV4'))}
function highlightTarget(t){clearTargets();if(!t)return;document.querySelector(`.terminal[data-node="${t.node}"][data-port="${t.port}"]`)?.classList.add('eaDropTargetV4')}
function setSelected(id){const a=A();if(!a)return;a.E.selected=+id;document.querySelectorAll('.eaComponent').forEach(e=>e.classList.toggle('selected',+e.dataset.id===+id));renderInspector()}
function badge(n){if(!n)return;const d=spec(n),el=document.querySelector(`.eaComponent[data-id="${n.id}"] .eaUnitBadge`);if(!el||!d)return;if(n.type==='switch')el.textContent=n.closed?'ON • مغلق':'OFF • مفتوح';else if(n.type==='resistor')el.textContent=`${n.value} Ω`;else if(n.type==='ldr'||n.type==='thermistor')el.textContent=`${n.value} kΩ`;else el.textContent=`${n.value} ${d.unit}`}
function renderInspector(){const a=A(),box=document.getElementById('eaInspector'),n=a&&node(a.E.selected);if(!box)return;if(!n){box.innerHTML='<div class="eaInspectorHead"><b>خصائص العنصر</b><span>اضغط قطعة لاختيارها</span></div>';return}const d=spec(n);const ctl=n.type==='switch'?`<button id="v4Toggle" class="secondary">${n.closed?'ON — مغلق':'OFF — مفتوح'}</button>`:`<div class="v4ValueRow"><input id="v4Value" type="number" value="${n.value}" min="${d.min??0}" max="${d.max??99999}" step="${d.step??1}"><b>${d.unit}</b></div>`;box.innerHTML=`<div class="eaInspectorHead"><b>${d.name}</b><span>${d.use}</span></div><div class="eaInspectorBody"><div class="v4InspectorGrid"><div><small>القيمة / الحالة</small>${ctl}</div><div><small>الأطراف</small><div class="v4Ports">${d.ports.map(p=>`<span>${p[1]}</span>`).join('')}</div></div></div><div class="eaHintBox">💡 ${d.use}</div><div class="eaInspectorActions"><button id="v4Focus">توسيط</button><button id="v4Delete" class="danger">حذف</button></div></div>`;
 const inp=document.getElementById('v4Value');if(inp)inp.oninput=()=>{const v=parseFloat(inp.value);if(Number.isFinite(v)){n.value=v;a.E.powered=false;badge(n);drawWires()}};
 const tog=document.getElementById('v4Toggle');if(tog)tog.onclick=()=>{n.closed=!n.closed;a.E.powered=false;renderInspector();badge(n)};
 document.getElementById('v4Delete').onclick=()=>{a.E.wires=a.E.wires.filter(w=>w.a!==n.id&&w.b!==n.id);a.E.nodes=a.E.nodes.filter(x=>x.id!==n.id);a.E.selected=null;document.querySelector(`.eaComponent[data-id="${n.id}"]`)?.remove();drawWires();renderInspector()};
 document.getElementById('v4Focus').onclick=()=>{const v=document.getElementById('eaViewport'),world=document.getElementById('eaWorld');if(!v||!world)return;a.E.panX=v.clientWidth/2-n.x*a.E.zoom;a.E.panY=v.clientHeight/2-n.y*a.E.zoom;world.style.transform=`translate(${a.E.panX}px,${a.E.panY}px) scale(${a.E.zoom})`};
}
function neutralizeOldHandlers(){const v=document.getElementById('eaViewport');if(v){v.onpointerdown=v.onpointermove=v.onpointerup=v.onpointercancel=null}document.querySelectorAll('.terminal').forEach(t=>t.onpointerdown=null);document.querySelectorAll('.eaComponent').forEach(el=>{el.onpointerdown=el.onpointermove=el.onpointerup=null;if(!el.querySelector('.eaDragSurfaceV4')){const h=document.createElement('div');h.className='eaDragSurfaceV4';el.appendChild(h)}})}
function startTouch(e){const a=A();if(!a||!e.touches.length)return;const t=e.touches[0],term=e.target.closest?.('.terminal'),comp=e.target.closest?.('.eaComponent');
 if(e.touches.length>=2){const t1=e.touches[0],t2=e.touches[1];session={type:'pinch',d:Math.hypot(t1.clientX-t2.clientX,t1.clientY-t2.clientY),z:a.E.zoom};e.preventDefault();return}
 if(term){const n=node(term.dataset.node),p=spec(n).ports.find(x=>x[0]===term.dataset.port);session={type:'wire',src:{node:n.id,port:term.dataset.port},temp:{a:portWorld(n,term.dataset.port),b:portWorld(n,term.dataset.port),cls:p?p[2]:'neutral'}};term.classList.add('eaSourcePortV4');e.preventDefault();return}
 if(comp){const n=node(comp.dataset.id),wp=screenToWorld(t.clientX,t.clientY);setSelected(n.id);session={type:'drag',node:n.id,offX:wp.x-n.x,offY:wp.y-n.y,el:comp};comp.classList.add('eaDraggingV4');e.preventDefault();return}
 session={type:'pan',x:t.clientX,y:t.clientY,px:a.E.panX,py:a.E.panY};e.preventDefault();
}
function moveTouch(e){const a=A();if(!a||!session)return;e.preventDefault();
 if(session.type==='pinch'&&e.touches.length>=2){const t1=e.touches[0],t2=e.touches[1],d=Math.hypot(t1.clientX-t2.clientX,t1.clientY-t2.clientY);a.E.zoom=clamp(session.z*d/session.d,.48,1.55);const world=document.getElementById('eaWorld');if(world)world.style.transform=`translate(${a.E.panX}px,${a.E.panY}px) scale(${a.E.zoom})`;const z=document.getElementById('eaZoomText');if(z)z.textContent=Math.round(a.E.zoom*100)+'%';return}
 const t=e.touches[0];if(!t)return;
 if(session.type==='drag'){const n=node(session.node);if(!n)return;const wp=screenToWorld(t.clientX,t.clientY);n.x=clamp(wp.x-session.offX,62,1138);n.y=clamp(wp.y-session.offY,62,788);session.el.style.left=n.x+'px';session.el.style.top=n.y+'px';a.E.powered=false;drawWires();return}
 if(session.type==='wire'){session.temp.b=screenToWorld(t.clientX,t.clientY);drawWires(session.temp);highlightTarget(closestPort(t.clientX,t.clientY,session.src));return}
 if(session.type==='pan'){a.E.panX=session.px+(t.clientX-session.x);a.E.panY=session.py+(t.clientY-session.y);const w=document.getElementById('eaWorld');if(w)w.style.transform=`translate(${a.E.panX}px,${a.E.panY}px) scale(${a.E.zoom})`}
}
function endTouch(e){const a=A();if(!a||!session)return;const changed=e.changedTouches&&e.changedTouches[0];if(session.type==='drag'){session.el?.classList.remove('eaDraggingV4');badge(node(session.node));drawWires()}
 if(session.type==='wire'){const x=changed?.clientX??0,y=changed?.clientY??0,dst=closestPort(x,y,session.src);if(dst&&dst.node!==session.src.node){const dupe=a.E.wires.some(w=>(w.a===session.src.node&&w.ap===session.src.port&&w.b===dst.node&&w.bp===dst.port)||(w.b===session.src.node&&w.bp===session.src.port&&w.a===dst.node&&w.ap===dst.port));if(!dupe){a.E.wires.push({a:session.src.node,ap:session.src.port,b:dst.node,bp:dst.port});toast('تم توصيل السلك ✓')}}else toast('قرّب نهاية السلك من نقطة ملونة ثم ارفع إصبعك.');document.querySelectorAll('.eaSourcePortV4').forEach(x=>x.classList.remove('eaSourcePortV4'));clearTargets();a.E.powered=false;drawWires()}
 session=null;
}
function initLabV4(){const v=document.getElementById('eaViewport'),world=document.getElementById('eaWorld');if(!v||!world||v.dataset.touchV4)return;v.dataset.touchV4='1';neutralizeOldHandlers();const mo=new MutationObserver(()=>neutralizeOldHandlers());mo.observe(world,{childList:true,subtree:true});v.addEventListener('touchstart',startTouch,{passive:false});v.addEventListener('touchmove',moveTouch,{passive:false});v.addEventListener('touchend',endTouch,{passive:false});v.addEventListener('touchcancel',endTouch,{passive:false});
 const note=document.createElement('div');note.className='eaTouchHelpV4';note.innerHTML='<b>تحكم باللمس</b><span>اسحب جسم القطعة لتحريكها • اسحب من النقطة الملونة لتوصيل سلك • اسحب الخلفية لتحريك اللوحة</span>';document.querySelector('.eaBoardShell')?.appendChild(note);renderInspector();
}
const oldLab=lab;lab=function(s){oldLab(s);setTimeout(initLabV4,0)};if(document.getElementById('eaViewport'))setTimeout(initLabV4,0);
window.EA_TOUCH_ENGINE_V4={initLabV4,screenToWorld,worldToScreen,drawWires};
})();
