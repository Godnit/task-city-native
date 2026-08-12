(function(){
'use strict';

const WW=1440, WH=720;
let vp=null, wrap=null, svg=null, shield=null;
let scale=1, minScale=1, camX=WW/2, camY=WH/2;
let raf=0, moving=false, one=null, pinch=null, lastBucket=-1, lastW=0, lastH=0;
let countries=[], cities=[], cache=new Map();

function isAR(){return typeof game!=='undefined' && game.lang==='ar'}
function tr(a,e){return isAR()?a:e}
function clamp(v,a,b){return Math.max(a,Math.min(b,v))}
function visible(){return vp&&vp.clientWidth>100&&vp.clientHeight>100&&document.getElementById('map')?.classList.contains('active')}

function injectStyle(){
 if(document.getElementById('p6style'))return;
 const s=document.createElement('style');s.id='p6style';s.textContent=`
#viewport{overflow:hidden!important;touch-action:none!important;overscroll-behavior:none!important;background:#2b7681!important}
#worldWrap{transform-origin:0 0!important;will-change:transform!important;backface-visibility:hidden!important;contain:layout style paint!important}
#world{shape-rendering:geometricPrecision}
#world .p2label{display:block!important;opacity:0;font-size:9px!important;stroke-width:1.35px!important;font-weight:700!important;pointer-events:none!important}
#world .p2city{display:none!important;pointer-events:none!important}
#world .p2city.p6show{display:block!important}
#world .p2cityName{font-size:7px!important;stroke-width:1.15px!important;font-weight:600!important}
#world .p2cityDot{r:1.5px;stroke-width:.55px!important}
#world .p2admin{display:none!important;pointer-events:none!important}
#world.p6admin .p2admin{display:block!important}
#viewport.p6moving #world .p2admin{display:none!important}
#viewport.p6moving #world{shape-rendering:optimizeSpeed}
#viewport.p6moving #world .p2city{display:none!important}
.p6shield{position:absolute;left:0;right:0;top:48px;bottom:58px;z-index:7;touch-action:none;background:transparent}
@media(max-height:480px){.p6shield{top:42px}}
.p6status{position:absolute;right:9px;top:140px;z-index:13;min-width:90px;text-align:center;background:#263d3adb;border:1px solid #667c77;border-radius:5px;padding:5px 7px;color:#cbd8d5;font-size:10px;pointer-events:none}
`;(document.head||document.documentElement).appendChild(s);
}

function recalc(){
 if(!vp)return false;
 const w=vp.clientWidth,h=vp.clientHeight;
 if(w<100||h<100)return false;
 lastW=w;lastH=h;
 // Fit the WORLD WIDTH exactly. This prevents the large blank strip that appeared at far zoom.
 minScale=w/WW;
 if(!Number.isFinite(minScale)||minScale<=0)minScale=1;
 return true;
}

function clampCamera(){
 if(!vp)return;
 const hw=vp.clientWidth/(2*scale),hh=vp.clientHeight/(2*scale);
 // Horizontal: never allow any empty area outside longitude -180..180.
 if(hw>=WW/2)camX=WW/2;
 else camX=clamp(camX,hw,WW-hw);
 // Vertical: also never expose empty area; center if world is shorter than viewport.
 if(hh>=WH/2)camY=WH/2;
 else camY=clamp(camY,hh,WH-hh);
}

function transform(){return{x:vp.clientWidth/2-camX*scale,y:vp.clientHeight/2-camY*scale}}
function ratio(){return scale/Math.max(minScale,.00001)}
function bucketFor(){const r=ratio();return r<1.3?0:r<1.75?1:r<2.4?2:r<3.3?3:r<4.6?4:r<6.4?5:6}
function nominalRatio(b){return [1,1.48,2.0,2.8,3.9,5.4,7.4][b]||7.4}

function render(){
 raf=0;if(!visible())return;
 clampCamera();const t=transform();
 wrap.style.transform=`translate3d(${t.x.toFixed(2)}px,${t.y.toFixed(2)}px,0) scale(${scale.toFixed(5)})`;
 const zt=document.getElementById('zoomText');if(zt)zt.textContent=Math.round(scale*100)+'%';
 const b=bucketFor();if(b!==lastBucket)applyBucket(b);
}
function queue(){if(!raf)raf=requestAnimationFrame(render)}

function fit(){
 if(!recalc())return;
 scale=minScale;camX=WW/2;camY=WH/2;moving=false;
 vp.classList.remove('p6moving');lastBucket=-1;render();
 const rc=document.getElementById('regionCard');if(rc)rc.classList.remove('show');
}
function screenWorld(x,y){return{x:camX+(x-vp.clientWidth/2)/scale,y:camY+(y-vp.clientHeight/2)/scale}}
function zoomAt(f,x,y){
 if(!visible()||!recalc())return;
 const a=screenWorld(x,y),max=Math.max(minScale*10,7.0),ns=clamp(scale*f,minScale,max);
 scale=ns;camX=a.x-(x-vp.clientWidth/2)/scale;camY=a.y-(y-vp.clientHeight/2)/scale;queue();
}
function setMoving(v){moving=v;vp.classList.toggle('p6moving',v);if(!v)queue()}

function polyScore(r){if(!r||r.length<3)return 0;let a=0;for(let i=0,j=r.length-1;i<r.length;j=i++)a+=r[j][0]*r[i][1]-r[i][0]*r[j][1];return Math.abs(a)}
function representative(g){
 if(!g)return null;const ps=g.type==='Polygon'?[g.coordinates]:g.type==='MultiPolygon'?g.coordinates:[];let ring=null,best=-1;
 for(const p of ps){const r=p&&p[0],s=polyScore(r);if(s>best){best=s;ring=r}}
 if(!ring||!ring.length)return null;
 // Average only the largest polygon, avoiding remote islands pulling labels away.
 let sx=0,sy=0,n=0,base=ring[0][0];
 for(const q of ring){let lon=q[0];while(lon-base>180)lon-=360;while(lon-base<-180)lon+=360;sx+=lon;sy+=q[1];n++}
 let lon=sx/n;while(lon>180)lon-=360;while(lon<-180)lon+=360;
 return{x:(lon+180)*4,y:(90-sy/n)*4,score:best};
}
function codeOf(p){let c=((p.ADM0_A3||p.ISO_A3||p.SOV_A3||p.GU_A3||p.BRK_A3||p.POSTAL||'')+'').toUpperCase();return c==='-99'?'':c}

function buildData(){
 countries=[];cities=[];cache.clear();
 const byCode=new Map();for(const f of ((window.NE_COUNTRIES&&NE_COUNTRIES.features)||[])){const p=f.properties||{},c=codeOf(p),r=representative(f.geometry);if(c&&r)byCode.set(c,r)}
 for(const el of svg.querySelectorAll('.p2label')){const code=el.dataset.p2code||'',r=byCode.get(code);if(!r)continue;el.setAttribute('x',r.x.toFixed(1));el.setAttribute('y',r.y.toFixed(1));countries.push({el,code,x:r.x,y:r.y,score:r.score,name:el.textContent||code})}
 countries.sort((a,b)=>b.score-a.score);
 const cityEls=[...svg.querySelectorAll('.p2city')],features=(window.NE_CITIES&&NE_CITIES.features)||[];let j=0;
 for(const f of features){const p=f.properties||{},g=f.geometry;if(!g||g.type!=='Point')continue;const name=isAR()?(p.NAME_AR||p.NAME||''):(p.NAMEPAR||p.NAME||p.NAMEASCII||'');if(!name)continue;const el=cityEls[j++];if(!el)break;const dot=el.querySelector('.p2cityDot');if(!dot)continue;cities.push({el,x:+dot.getAttribute('cx'),y:+dot.getAttribute('cy'),rank:Number(p.SCALERANK??9)||9,name})}
 cities.sort((a,b)=>a.rank-b.rank);
 lastBucket=-1;
}

function cacheKey(b){return (isAR()?'ar':'en')+'|'+b}
function computeBucket(b){
 const key=cacheKey(b);if(cache.has(key))return cache.get(key);
 const nr=nominalRatio(b),screenScale=minScale*nr;
 // Collision is calculated ONCE for the whole world at this zoom bucket, never while panning.
 const cellW=58/screenScale,cellH=20/screenScale,occ=new Set(),countryCodes=new Set(),cityIdx=new Set();
 const maxCountries=[52,78,110,145,180,220,260][b];let shown=0;
 const forceCode=typeof game!=='undefined'?game.me:'';
 function reserve(x,y,w,h){const x0=Math.floor((x-w/2)/cellW),x1=Math.floor((x+w/2)/cellW),y0=Math.floor((y-h/2)/cellH),y1=Math.floor((y+h/2)/cellH);for(let gx=x0;gx<=x1;gx++)for(let gy=y0;gy<=y1;gy++)if(occ.has(gx+':'+gy))return false;for(let gx=x0;gx<=x1;gx++)for(let gy=y0;gy<=y1;gy++)occ.add(gx+':'+gy);return true}
 for(const o of countries){if(shown>=maxCountries&&o.code!==forceCode)continue;const chars=(o.el.textContent||o.name||'').length,w=Math.max(38,Math.min(115,chars*(isAR()?5.3:4.8)+12))/screenScale,h=17/screenScale;if(!reserve(o.x,o.y,w,h)&&o.code!==forceCode)continue;countryCodes.add(o.code);shown++}
 if(b>=3){const rankLimit=[-1,-1,-1,1,2,3,5][b],maxCities=[0,0,0,24,42,68,105][b];let count=0;for(let i=0;i<cities.length&&count<maxCities;i++){const c=cities[i];if(c.rank>rankLimit)continue;const w=Math.max(40,Math.min(118,c.name.length*(isAR()?5.2:4.7)+16))/screenScale,h=16/screenScale;if(!reserve(c.x,c.y,w,h))continue;cityIdx.add(i);count++}}
 const out={countryCodes,cityIdx};cache.set(key,out);return out;
}

function applyBucket(b){
 if(!svg)return;lastBucket=b;const snap=computeBucket(b),nr=nominalRatio(b);
 // Font sizes only change when crossing a zoom bucket. Panning does zero label-layout work.
 const targetScreenCountry=b<2?10:b<4?9.4:8.8,targetScreenCity=b<5?8.0:7.4;
 const worldCountry=targetScreenCountry/(minScale*nr),worldCity=targetScreenCity/(minScale*nr);
 for(const o of countries){o.el.style.opacity=snap.countryCodes.has(o.code)?'.96':'0';o.el.style.fontSize=worldCountry.toFixed(2)+'px';o.el.style.strokeWidth=(1.35/(minScale*nr)).toFixed(2)+'px'}
 for(let i=0;i<cities.length;i++){const o=cities[i],on=snap.cityIdx.has(i);o.el.classList.toggle('p6show',on);if(on){const tx=o.el.querySelector('.p2cityName'),dot=o.el.querySelector('.p2cityDot');if(tx){tx.style.fontSize=worldCity.toFixed(2)+'px';tx.style.strokeWidth=(1.1/(minScale*nr)).toFixed(2)+'px'}if(dot)dot.setAttribute('r',(1.55/(minScale*nr)).toFixed(2))}}
 svg.classList.toggle('p6admin',b>=3);
 const st=document.getElementById('p6status');if(st)st.textContent=b<3?tr('الدول','Countries'):b<5?tr('الأقاليم','Provinces'):tr('المدن والأقاليم','Cities & provinces');
}

function underneath(cx,cy){shield.style.pointerEvents='none';const el=document.elementFromPoint(cx,cy);shield.style.pointerEvents='auto';return el}
function installInput(){
 shield=document.createElement('div');shield.className='p6shield';document.getElementById('map').appendChild(shield);
 shield.addEventListener('touchstart',e=>{e.preventDefault();if(!visible())return;if(vp.clientWidth!==lastW||vp.clientHeight!==lastH)fit();setMoving(true);if(e.touches.length===1){const t=e.touches[0],r=vp.getBoundingClientRect(),x=t.clientX-r.left,y=t.clientY-r.top;one={x,y,sx:x,sy:y,cx:t.clientX,cy:t.clientY,m:false};pinch=null}else{const a=e.touches[0],b=e.touches[1],r=vp.getBoundingClientRect(),ax=a.clientX-r.left,ay=a.clientY-r.top,bx=b.clientX-r.left,by=b.clientY-r.top,mx=(ax+bx)/2,my=(ay+by)/2,A=screenWorld(mx,my);pinch={d:Math.max(3,Math.hypot(ax-bx,ay-by)),s:scale,wx:A.x,wy:A.y};one=null}},{passive:false});
 shield.addEventListener('touchmove',e=>{e.preventDefault();if(e.touches.length===1&&one){const t=e.touches[0],r=vp.getBoundingClientRect(),x=t.clientX-r.left,y=t.clientY-r.top,dx=x-one.x,dy=y-one.y;if(Math.hypot(x-one.sx,y-one.sy)>6)one.m=true;camX-=dx/scale;camY-=dy/scale;one.x=x;one.y=y;one.cx=t.clientX;one.cy=t.clientY;queue()}else if(e.touches.length>=2&&pinch){const a=e.touches[0],b=e.touches[1],r=vp.getBoundingClientRect(),ax=a.clientX-r.left,ay=a.clientY-r.top,bx=b.clientX-r.left,by=b.clientY-r.top,mx=(ax+bx)/2,my=(ay+by)/2,d=Math.max(3,Math.hypot(ax-bx,ay-by));scale=clamp(pinch.s*d/pinch.d,minScale,Math.max(minScale*10,7));camX=pinch.wx-(mx-vp.clientWidth/2)/scale;camY=pinch.wy-(my-vp.clientHeight/2)/scale;queue()}},{passive:false});
 shield.addEventListener('touchend',e=>{e.preventDefault();if(e.touches.length===0){if(one&&!one.m){const el=underneath(one.cx,one.cy),p=el&&el.closest?el.closest('.p2country'):null;if(p&&window.p2SelectCountry)window.p2SelectCountry(p.dataset.code)}one=null;pinch=null;setMoving(false)}else if(e.touches.length===1){const t=e.touches[0],r=vp.getBoundingClientRect(),x=t.clientX-r.left,y=t.clientY-r.top;one={x,y,sx:x,sy:y,cx:t.clientX,cy:t.clientY,m:true};pinch=null}},{passive:false});
 shield.addEventListener('wheel',e=>{e.preventDefault();const r=vp.getBoundingClientRect();zoomAt(e.deltaY<0?1.16:.86,e.clientX-r.left,e.clientY-r.top)},{passive:false});
}

function tools(){const plus=document.getElementById('p2plus'),minus=document.getElementById('p2minus'),home=document.getElementById('p2home'),focus=document.getElementById('p2focus');if(plus)plus.onclick=()=>zoomAt(1.3,vp.clientWidth/2,vp.clientHeight/2);if(minus)minus.onclick=()=>zoomAt(.77,vp.clientWidth/2,vp.clientHeight/2);if(home)home.onclick=fit;if(focus)focus.onclick=()=>{const me=countries.find(x=>typeof game!=='undefined'&&x.code===game.me);if(!me){fit();return}scale=Math.max(minScale*3.0,minScale);camX=me.x;camY=me.y;queue()}}
function activate(){let n=0;const go=()=>{n++;if(visible()){recalc();buildData();fit();return}if(n<25)setTimeout(go,45)};go()}
function install(){
 vp=document.getElementById('viewport');wrap=document.getElementById('worldWrap');svg=document.getElementById('world');if(!vp||!wrap||!svg||!svg.querySelector('.p2country')){setTimeout(install,70);return}
 injectStyle();for(const id of ['p3layer','p4layer','p5status'])document.getElementById(id)?.remove();let st=document.getElementById('p6status');if(!st){st=document.createElement('div');st.id='p6status';st.className='p6status';document.getElementById('map').appendChild(st)}
 buildData();installInput();tools();
 const oldShow=window.show;if(typeof oldShow==='function')window.show=function(id){oldShow(id);if(id==='map'){setTimeout(activate,0);setTimeout(activate,120)}};
 const oldStart=window.startGame;if(typeof oldStart==='function')window.startGame=function(){oldStart();setTimeout(activate,150)};
 const oldContinue=window.continueGame;if(typeof oldContinue==='function')window.continueGame=function(){oldContinue();setTimeout(activate,120)};
 const oldLang=window.setLang;if(typeof oldLang==='function')window.setLang=function(l){oldLang(l);setTimeout(()=>{buildData();lastBucket=-1;queue()},80)};
 window.resetMap=fit;window.applyTransform=queue;window.addEventListener('resize',()=>setTimeout(fit,80));if(visible())activate();
}
setTimeout(install,120);
})();
