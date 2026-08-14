(function(){
'use strict';
if(window.__p20FinalMap)return;window.__p20FinalMap=true;

const NS='http://www.w3.org/2000/svg';
const WW=1440, WH=720, MAX_ZOOM=32;
let vp=null, world=null, wrap=null, zoomText=null;
let zoom=1, camX=WW/2, camY=WH/2, baseW=WW, baseH=WH;
let drag=null, pinch=null, moved=false, raf=0, installed=false, firstFit=false;
let countryLabels=[], cityRows=[];

const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
const isArabic=()=>!window.game || game.lang==='ar';
const project=(lon,lat)=>({x:(lon+180)*4,y:(90-lat)*4});

function active(){return !!(vp&&document.getElementById('map')?.classList.contains('active')&&vp.clientWidth>80&&vp.clientHeight>80)}

function calcBase(){
  if(!vp||vp.clientWidth<80||vp.clientHeight<80)return false;
  const a=vp.clientWidth/vp.clientHeight, wa=WW/WH;
  if(a>=wa){baseH=WH;baseW=WH*a}else{baseW=WW;baseH=WW/a}
  return Number.isFinite(baseW)&&Number.isFinite(baseH)&&baseW>0&&baseH>0;
}

function view(){return {w:baseW/zoom,h:baseH/zoom}}

function clampCamera(){
  const v=view();
  camX=v.w>=WW?WW/2:clamp(camX,v.w/2,WW-v.w/2);
  camY=v.h>=WH?WH/2:clamp(camY,v.h/2,WH-v.h/2);
}

function render(){
  raf=0;if(!world||!calcBase())return;
  clampCamera();
  const v=view(), x=camX-v.w/2, y=camY-v.h/2;
  world.setAttribute('viewBox',`${x.toFixed(4)} ${y.toFixed(4)} ${v.w.toFixed(4)} ${v.h.toFixed(4)}`);
  world.setAttribute('preserveAspectRatio','none');
  if(zoomText)zoomText.textContent=Math.round(zoom*100)+'%';
}
function queue(){if(!raf)raf=requestAnimationFrame(render)}

function screenToWorld(x,y){
  const v=view();
  return {x:camX-v.w/2+(x/vp.clientWidth)*v.w,y:camY-v.h/2+(y/vp.clientHeight)*v.h};
}

function fit(){
  if(!calcBase())return;
  zoom=1;camX=WW/2;camY=WH/2;drag=null;pinch=null;moved=false;
  render();refreshVisuals(true);document.getElementById('regionCard')?.classList.remove('show');firstFit=true;
}

function setZoom(nz,screenX,screenY,finish){
  if(!calcBase())return;
  const sx=Number.isFinite(screenX)?screenX:vp.clientWidth/2;
  const sy=Number.isFinite(screenY)?screenY:vp.clientHeight/2;
  const anchor=screenToWorld(sx,sy);
  zoom=clamp(nz,1,MAX_ZOOM);
  const v=view();
  camX=anchor.x+(0.5-sx/vp.clientWidth)*v.w;
  camY=anchor.y+(0.5-sy/vp.clientHeight)*v.h;
  render();if(finish)refreshVisuals(false);
}
function zoomBy(f,x,y){setZoom(zoom*f,x,y,true)}

function focusPlayer(){
  const code=(window.game&&game.me)||'YEM';
  const p=world?.querySelector(`.p2country[data-code="${code}"]`);
  if(!p)return fit();
  try{
    const b=p.getBBox();
    if(!b.width||!b.height)return fit();
    if(!calcBase())return;
    const target=Math.min(baseW/Math.max(35,b.width*3.1),baseH/Math.max(28,b.height*3.1));
    zoom=clamp(target,1.8,12);camX=b.x+b.width/2;camY=b.y+b.height/2;
    render();refreshVisuals(true);
  }catch(_){fit()}
}

function addStyle(){
  if(document.getElementById('p20FinalStyle'))return;
  const s=document.createElement('style');s.id='p20FinalStyle';s.textContent=`
#viewport{direction:ltr!important;overflow:hidden!important;touch-action:none!important;overscroll-behavior:none!important;background:#2b7681!important}
#viewport>#worldWrap{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;transform:none!important;transform-origin:0 0!important;direction:ltr!important}
#world{display:block!important;width:100%!important;height:100%!important;overflow:hidden!important;shape-rendering:geometricPrecision!important;text-rendering:geometricPrecision!important;background:#2b7681!important}
#world .p2country{stroke:#263b3a!important;stroke-width:.48px!important;vector-effect:non-scaling-stroke!important;stroke-linejoin:round!important;stroke-linecap:round!important;shape-rendering:geometricPrecision!important}
#world .p2country.p2selected{stroke:#f3df8c!important;stroke-width:1.25px!important;filter:brightness(1.08)!important}
#world .p2grid{stroke-width:.25px!important;opacity:.08!important;vector-effect:non-scaling-stroke!important}
#world .p2admin{stroke:#435654!important;stroke-width:.28px!important;opacity:.65!important;vector-effect:non-scaling-stroke!important;shape-rendering:geometricPrecision!important}
#world .p2label,#world .p2cityName,#world .p2hist,#p20Seas text{text-rendering:geometricPrecision!important;paint-order:stroke!important;pointer-events:none!important}
#world .p2city{display:none!important;pointer-events:none!important}
#world .p2cityDot{stroke-width:.45px!important;vector-effect:non-scaling-stroke!important}
#world .p2sea{display:none!important}
#p20Seas{pointer-events:none!important}
#p20Seas text{fill:#c7e4e5!important;stroke:#2b7681!important;font-style:italic;font-weight:500;text-anchor:middle;opacity:.28}
#p20YemAdm1,#p20YemAdm2{fill:none;pointer-events:none}
#p20YemAdm1 path{stroke:#304947;stroke-width:.34px;opacity:.9;vector-effect:non-scaling-stroke}
#p20YemAdm2 path{stroke:#51615e;stroke-width:.20px;opacity:.72;vector-effect:non-scaling-stroke}
.p2tools{z-index:20!important;right:10px!important}
.p2tool{touch-action:manipulation!important}
.p20close{position:absolute;top:5px;left:5px;width:28px;height:28px;border:1px solid #7b918b;border-radius:6px;background:#263b39;color:#e8eeee;font-size:18px;line-height:24px;z-index:2}
`;(document.head||document.documentElement).appendChild(s);
}

function geomPath(g){
  if(!g)return'';
  const ring=r=>{if(!r||!r.length)return'';let p=project(r[0][0],r[0][1]),d=`M${p.x.toFixed(2)},${p.y.toFixed(2)}`;for(let i=1;i<r.length;i++){p=project(r[i][0],r[i][1]);d+=`L${p.x.toFixed(2)},${p.y.toFixed(2)}`}return d+'Z'};
  if(g.type==='Polygon')return g.coordinates.map(ring).join('');
  if(g.type==='MultiPolygon')return g.coordinates.map(p=>p.map(ring).join('')).join('');
  if(g.type==='LineString'){const a=g.coordinates;if(!a?.length)return'';let p=project(a[0][0],a[0][1]),d=`M${p.x.toFixed(2)},${p.y.toFixed(2)}`;for(let i=1;i<a.length;i++){p=project(a[i][0],a[i][1]);d+=`L${p.x.toFixed(2)},${p.y.toFixed(2)}`}return d}
  if(g.type==='MultiLineString')return g.coordinates.map(a=>geomPath({type:'LineString',coordinates:a})).join('');
  return'';
}

function buildYemenDetail(){
  if(!world||document.getElementById('p20YemAdm1')||!window.YEM_ADMIN)return;
  const make=(id,fc)=>{const g=document.createElementNS(NS,'g');g.id=id;(fc?.features||[]).forEach(f=>{const d=geomPath(f.geometry);if(!d)return;const p=document.createElementNS(NS,'path');p.setAttribute('d',d);g.appendChild(p)});world.appendChild(g);return g};
  make('p20YemAdm1',YEM_ADMIN.adm1);make('p20YemAdm2',YEM_ADMIN.adm2);
}

function buildSeaLabels(){
  world.querySelectorAll('.p2sea').forEach(e=>e.style.setProperty('display','none','important'));
  let g=document.getElementById('p20Seas');if(g)return g;
  g=document.createElementNS(NS,'g');g.id='p20Seas';
  const defs=[
    ['atlantic','المحيط الأطلسي','Atlantic Ocean',-36,18],
    ['med','البحر المتوسط','Mediterranean Sea',17,35],
    ['black','البحر الأسود','Black Sea',35,43],
    ['red','البحر الأحمر','Red Sea',38.5,20],
    ['arabian','بحر العرب','Arabian Sea',63,13],
    ['indian','المحيط الهندي','Indian Ocean',82,-19]
  ];
  defs.forEach(([key,ar,en,lo,la])=>{const p=project(lo,la),t=document.createElementNS(NS,'text');t.dataset.key=key;t.dataset.ar=ar;t.dataset.en=en;t.textContent=isArabic()?ar:en;t.setAttribute('x',p.x);t.setAttribute('y',p.y);g.appendChild(t)});
  world.appendChild(g);return g;
}

function prepareLabels(){
  countryLabels=[...world.querySelectorAll('.p2label')].map(e=>{
    const code=e.dataset.p2code||'',p=world.querySelector(`.p2country[data-code="${code}"]`);let area=0;
    try{const b=p?.getBBox();if(b)area=b.width*b.height}catch(_){}
    return {e,area,x:+e.getAttribute('x')||0,y:+e.getAttribute('y')||0};
  }).sort((a,b)=>b.area-a.area);
  const groups=[...world.querySelectorAll('.p2city')], fs=(window.NE_CITIES&&NE_CITIES.features)||[];
  cityRows=groups.map((e,i)=>{const p=fs[i]?.properties||{},dot=e.querySelector('.p2cityDot'),tx=e.querySelector('.p2cityName');return {e,dot,tx,rank:Number(p.SCALERANK??9)||9,ar:p.NAME_AR||'',en:p.NAMEPAR||p.NAME||p.NAMEASCII||'',x:+(dot?.getAttribute('cx')||0),y:+(dot?.getAttribute('cy')||0)}});
}

function boxHits(a,boxes){for(const b of boxes){if(a.x<b.x+b.w&&a.x+a.w>b.x&&a.y<b.y+b.h&&a.y+a.h>b.y)return true}return false}

function refreshVisuals(force){
  if(!world||!vp||!calcBase())return;
  const v=view(), left=camX-v.w/2, top=camY-v.h/2, sx=vp.clientWidth/v.w;
  const labelPx=zoom<1.6?11:zoom<2.5?10:zoom<4?9:0;
  const boxes=[];
  countryLabels.forEach(o=>{
    const e=o.e;
    if(labelPx<=0||o.x<left||o.x>left+v.w||o.y<top||o.y>top+v.h){e.style.setProperty('display','none','important');return}
    const txt=(e.textContent||'').trim();if(isArabic()&&/[A-Za-z]/.test(txt)){e.style.setProperty('display','none','important');return}
    const px=(o.x-left)*sx, py=(o.y-top)*sx, w=Math.max(30,Math.min(150,txt.length*7+12)),h=16,box={x:px-w/2,y:py-h/2,w,h};
    if(boxHits(box,boxes)){e.style.setProperty('display','none','important');return}
    boxes.push(box);e.style.setProperty('display','block','important');e.style.setProperty('opacity',zoom>2.8?'.72':'.96','important');e.style.setProperty('font-size',(labelPx/sx).toFixed(4)+'px','important');e.style.setProperty('stroke-width',(1.6/sx).toFixed(4)+'px','important');
  });

  let rankLimit=-1;if(zoom>=3.6&&zoom<5.5)rankLimit=1;else if(zoom>=5.5&&zoom<8)rankLimit=2;else if(zoom>=8&&zoom<13)rankLimit=4;else if(zoom>=13)rankLimit=8;
  const cityBoxes=[];
  cityRows.forEach(o=>{
    const nm=isArabic()?o.ar:o.en;
    if(rankLimit<0||o.rank>rankLimit||!nm||(isArabic()&&/[A-Za-z]/.test(nm))||o.x<left||o.x>left+v.w||o.y<top||o.y>top+v.h){o.e.style.setProperty('display','none','important');return}
    const px=(o.x-left)*sx,py=(o.y-top)*sx,w=Math.max(34,Math.min(140,nm.length*7+14)),h=17,box={x:px-w/2,y:py-h-5,w,h};
    if(boxHits(box,cityBoxes)){o.e.style.setProperty('display','none','important');return}
    cityBoxes.push(box);o.e.style.setProperty('display','block','important');
    if(o.tx){o.tx.textContent=nm;o.tx.style.setProperty('font-size',(8/sx).toFixed(4)+'px','important');o.tx.style.setProperty('stroke-width',(1.5/sx).toFixed(4)+'px','important');o.tx.setAttribute('y',(o.y-5/sx).toFixed(4))}
    if(o.dot)o.dot.setAttribute('r',(1.5/sx).toFixed(4));
  });

  world.querySelectorAll('.p2hist').forEach(e=>{const on=zoom<1.8;e.style.setProperty('display',on?'block':'none','important');if(on){e.style.setProperty('font-size',(13/sx).toFixed(4)+'px','important');e.style.setProperty('stroke-width',(1.5/sx).toFixed(4)+'px','important')}});
  const seas=buildSeaLabels();seas.querySelectorAll('text').forEach(e=>{e.textContent=isArabic()?e.dataset.ar:e.dataset.en;const on=zoom<2.25;e.style.setProperty('display',on?'block':'none','important');if(on){e.style.setProperty('font-size',((zoom<1.4?16:14)/sx).toFixed(4)+'px','important');e.style.setProperty('stroke-width',(1.2/sx).toFixed(4)+'px','important')}});
  const y1=document.getElementById('p20YemAdm1'),y2=document.getElementById('p20YemAdm2');if(y1)y1.style.display=zoom>=4?'block':'none';if(y2)y2.style.display=zoom>=8?'block':'none';
  world.querySelectorAll('.p2admin').forEach(e=>e.style.setProperty('display',zoom>=2.1?'block':'none','important'));
}

function localTouch(t){const r=vp.getBoundingClientRect();return {x:t.clientX-r.left,y:t.clientY-r.top}}

function installInput(){
  vp.addEventListener('touchstart',e=>{
    if(e.touches.length===1){e.preventDefault();const p=localTouch(e.touches[0]);drag={x:p.x,y:p.y,sx:p.x,sy:p.y,target:e.target?.closest?.('.p2country')||null};pinch=null;moved=false}
    else if(e.touches.length===2){e.preventDefault();const a=localTouch(e.touches[0]),b=localTouch(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2;pinch={dist:Math.max(1,Math.hypot(a.x-b.x,a.y-b.y)),startZoom:zoom,anchor:screenToWorld(mx,my)};drag=null;moved=true}
  },{passive:false});
  vp.addEventListener('touchmove',e=>{
    e.preventDefault();
    if(e.touches.length===1&&drag){const p=localTouch(e.touches[0]),dx=p.x-drag.x,dy=p.y-drag.y;if(Math.hypot(p.x-drag.sx,p.y-drag.sy)>5)moved=true;const v=view();camX-=dx*v.w/vp.clientWidth;camY-=dy*v.h/vp.clientHeight;drag.x=p.x;drag.y=p.y;queue()}
    else if(e.touches.length===2&&pinch){const a=localTouch(e.touches[0]),b=localTouch(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2,d=Math.max(1,Math.hypot(a.x-b.x,a.y-b.y));zoom=clamp(pinch.startZoom*d/pinch.dist,1,MAX_ZOOM);const v=view();camX=pinch.anchor.x+(0.5-mx/vp.clientWidth)*v.w;camY=pinch.anchor.y+(0.5-my/vp.clientHeight)*v.h;queue()}
  },{passive:false});
  vp.addEventListener('touchend',e=>{
    if(e.touches.length===0){const tap=drag&&!moved?drag.target:null;drag=null;pinch=null;render();refreshVisuals(false);if(tap?.dataset?.code&&typeof window.p2SelectCountry==='function')window.p2SelectCountry(tap.dataset.code)}
    else if(e.touches.length===1){const p=localTouch(e.touches[0]);drag={x:p.x,y:p.y,sx:p.x,sy:p.y,target:e.target?.closest?.('.p2country')||null};pinch=null;moved=false}
  },{passive:false});
  vp.addEventListener('touchcancel',()=>{drag=null;pinch=null;render();refreshVisuals(false)},{passive:false});
  vp.addEventListener('wheel',e=>{e.preventDefault();const r=vp.getBoundingClientRect();zoomBy(e.deltaY<0?1.18:1/1.18,e.clientX-r.left,e.clientY-r.top)},{passive:false});
  vp.addEventListener('dblclick',e=>{const r=vp.getBoundingClientRect();zoomBy(1.55,e.clientX-r.left,e.clientY-r.top)});
}

function wireTools(){
  const ids=['p2plus','p2minus','p2home','p2focus'];const fresh=[];
  ids.forEach(id=>{const old=document.getElementById(id);if(!old)return;const n=old.cloneNode(true);old.replaceWith(n);fresh.push(n)});
  document.getElementById('p2plus')?.addEventListener('click',()=>zoomBy(1.3,vp.clientWidth/2,vp.clientHeight/2));
  document.getElementById('p2minus')?.addEventListener('click',()=>zoomBy(1/1.3,vp.clientWidth/2,vp.clientHeight/2));
  document.getElementById('p2home')?.addEventListener('click',fit);
  document.getElementById('p2focus')?.addEventListener('click',focusPlayer);
}

function addClose(){
  const card=document.getElementById('regionCard');if(!card||card.querySelector('.p20close'))return;
  const b=document.createElement('button');b.type='button';b.className='p20close';b.textContent='×';b.setAttribute('aria-label','إلغاء التحديد');b.addEventListener('click',()=>{world.querySelectorAll('.p2selected').forEach(e=>e.classList.remove('p2selected'));card.classList.remove('show')});card.appendChild(b);
}

function patchLanguage(){
  const old=window.setLang;if(typeof old==='function'&&!old.__p20){const f=function(l){old(l);setTimeout(()=>{prepareLabels();refreshVisuals(true)},60)};f.__p20=true;window.setLang=f}
}
function patchNavigation(){
  const oldShow=window.show;if(typeof oldShow==='function'&&!oldShow.__p20){const f=function(id){oldShow(id);if(id==='map')setTimeout(()=>{render();if(!firstFit)fit();else refreshVisuals(true)},60)};f.__p20=true;window.show=f}
  const oldStart=window.startGame;if(typeof oldStart==='function'&&!oldStart.__p20){const f=function(){oldStart();setTimeout(fit,180)};f.__p20=true;window.startGame=f}
  const oldCont=window.continueGame;if(typeof oldCont==='function'&&!oldCont.__p20){const f=function(){oldCont();setTimeout(fit,180)};f.__p20=true;window.continueGame=f}
}

function install(){
  if(installed)return;
  const old=document.getElementById('viewport'),oldWorld=old?.querySelector('#world');
  if(!old||!oldWorld||!oldWorld.querySelector('.p2country'))return setTimeout(install,120);
  addStyle();
  const clone=old.cloneNode(true);old.replaceWith(clone);vp=clone;wrap=vp.querySelector('#worldWrap');world=vp.querySelector('#world');zoomText=document.getElementById('zoomText');
  wrap.style.transform='none';world.setAttribute('viewBox','0 0 1440 720');world.removeAttribute('width');world.removeAttribute('height');world.setAttribute('preserveAspectRatio','none');
  buildYemenDetail();buildSeaLabels();prepareLabels();installInput();wireTools();addClose();patchLanguage();patchNavigation();
  window.resetMap=fit;window.applyTransform=render;window.__mapDebug=()=>({zoom,camX,camY,baseW,baseH,viewBox:world.getAttribute('viewBox'),countries:world.querySelectorAll('.p2country').length,admins:world.querySelectorAll('.p2admin').length,cities:world.querySelectorAll('.p2city').length});
  window.addEventListener('resize',()=>setTimeout(()=>{if(!calcBase())return;if(zoom<=1.02)fit();else{render();refreshVisuals(true)}},80));
  installed=true;setTimeout(()=>{fit()},40);
}
setTimeout(install,80);
})();
