(function(){
'use strict';

const WORLD_W=1440, WORLD_H=720;
let vp=null, wrap=null, svg=null, overlay=null;
let scale=1, camX=WORLD_W/2, camY=WORLD_H/2;
let minScale=0.5, raf=0, labelTimer=0, moving=false;
let single=null, pinch=null, tapTarget=null, tapMoved=false;
let countryLabels=[], cityLabels=[];

function isArabic(){return typeof game!=='undefined' && game.lang==='ar'}
function tr(a,e){return isArabic()?a:e}
function clamp(v,a,b){return Math.max(a,Math.min(b,v))}
function localTouch(t){const r=vp.getBoundingClientRect();return{x:t.clientX-r.left,y:t.clientY-r.top}}
function screenToWorld(x,y){return{x:camX+(x-vp.clientWidth/2)/scale,y:camY+(y-vp.clientHeight/2)/scale}}
function zoomRatio(){return scale/Math.max(minScale,0.0001)}

function injectStyle(){
 if(document.getElementById('phase4CameraStyle'))return;
 const s=document.createElement('style');
 s.id='phase4CameraStyle';
 s.textContent=`
#viewport{position:relative!important;overflow:hidden!important;touch-action:none!important;overscroll-behavior:none!important;background:#2b7681!important}
#worldWrap{transform-origin:0 0!important;will-change:transform!important;backface-visibility:hidden!important;contain:layout style paint!important}
#world{shape-rendering:geometricPrecision}
#world .p2label,#world .p2city,#world .p2hist,#world .p2sea{display:none!important}
#world .p2admin{display:none!important}
#world.p4-show-admin .p2admin{display:block!important}
#viewport.p4-moving #world .p2admin{display:none!important}
#viewport.p4-moving #world{shape-rendering:optimizeSpeed}
#viewport.p4-moving .p4-overlay{opacity:.18}
.p4-overlay{position:absolute;inset:0;z-index:6;pointer-events:none;overflow:hidden;transition:opacity .08s linear}
.p4-country-label,.p4-city-label{position:absolute;left:0;top:0;transform:translate(-50%,-50%);white-space:nowrap;line-height:1;user-select:none;pointer-events:none;text-shadow:-1px -1px 0 #223332,1px -1px 0 #223332,-1px 1px 0 #223332,1px 1px 0 #223332,0 1px 2px #142423}
.p4-country-label{font-size:11px;font-weight:700;color:#edf3f1}
.p4-country-label.small{font-size:9px;font-weight:650}
.p4-city-label{font-size:9px;font-weight:600;color:#f1f3f2;padding-left:8px}
.p4-city-label:before{content:'';position:absolute;width:5px;height:5px;border-radius:50%;background:#efd681;border:1px solid #263936;left:0;top:50%;transform:translateY(-50%)}
.p4-layer{position:absolute;right:9px;top:140px;z-index:13;background:#263d3adb;border:1px solid #667c77;border-radius:5px;padding:5px 7px;color:#cbd8d5;font-size:10px;pointer-events:none;min-width:82px;text-align:center}
.p4-edgehint{position:absolute;left:50%;bottom:8px;transform:translateX(-50%);z-index:13;background:#1f3533b8;border:1px solid #5d736e;border-radius:12px;padding:3px 9px;color:#bfcfcb;font-size:9px;pointer-events:none;opacity:0;transition:opacity .15s}
.p4-edgehint.show{opacity:1}
`;
 document.head.appendChild(s);
}

function recalcMin(){
 if(!vp)return;
 const pad=10;
 minScale=Math.min((vp.clientWidth-pad*2)/WORLD_W,(vp.clientHeight-pad*2)/WORLD_H);
 minScale=Math.max(0.12,minScale);
}

function clampCamera(){
 const halfW=vp.clientWidth/(2*scale), halfH=vp.clientHeight/(2*scale);
 const freeX=Math.min(110/scale,WORLD_W*0.10);
 const freeY=Math.min(70/scale,WORLD_H*0.08);
 if(halfW>=WORLD_W/2){camX=clamp(camX,WORLD_W/2-freeX,WORLD_W/2+freeX)}
 else camX=clamp(camX,halfW,WORLD_W-halfW);
 if(halfH>=WORLD_H/2){camY=clamp(camY,WORLD_H/2-freeY,WORLD_H/2+freeY)}
 else camY=clamp(camY,halfH,WORLD_H-halfH);
}

function cameraTransform(){
 const tx=vp.clientWidth/2-camX*scale;
 const ty=vp.clientHeight/2-camY*scale;
 return{tx,ty};
}

function detailLevel(){
 const r=zoomRatio();
 if(r<1.45)return 0;
 if(r<2.25)return 1;
 if(r<3.35)return 2;
 if(r<4.9)return 3;
 return 4;
}

function applyDetailState(){
 const l=detailLevel();
 svg.classList.toggle('p4-show-admin',l>=2);
 const badge=document.getElementById('p4layer');
 if(badge)badge.textContent=l<2?tr('الدول','Countries'):l<3?tr('الحدود الداخلية','Internal borders'):tr('الأقاليم والمدن','Provinces & cities');
 const p3=document.getElementById('p3layer');if(p3)p3.style.display='none';
}

function render(updateLabelsNow){
 raf=0;
 if(!vp||!wrap)return;
 clampCamera();
 const t=cameraTransform();
 wrap.style.transform=`translate3d(${t.tx.toFixed(2)}px,${t.ty.toFixed(2)}px,0) scale(${scale.toFixed(5)})`;
 const zt=document.getElementById('zoomText');if(zt)zt.textContent='ZOOM '+Math.round(scale*100)+'%';
 if(!moving)applyDetailState();
 if(updateLabelsNow&&!moving)updateLabels();
}
function queueRender(labels){
 if(labels){clearTimeout(labelTimer);labelTimer=setTimeout(()=>{labelTimer=0;if(!moving)updateLabels()},35)}
 if(!raf)raf=requestAnimationFrame(()=>render(false));
}

function reset(){
 recalcMin();scale=minScale;camX=WORLD_W/2;camY=WORLD_H/2;moving=false;
 vp.classList.remove('p4-moving');applyDetailState();render(true);
 const rc=document.getElementById('regionCard');if(rc)rc.classList.remove('show');
}

function setMoving(v){
 moving=v;
 vp.classList.toggle('p4-moving',v);
 if(!v){applyDetailState();queueRender(true)}
}

function zoomAt(f,x,y){
 const anchor=screenToWorld(x,y);
 const ns=clamp(scale*f,minScale,Math.max(7.8,minScale*11));
 scale=ns;
 camX=anchor.x-(x-vp.clientWidth/2)/scale;
 camY=anchor.y-(y-vp.clientHeight/2)/scale;
 queueRender(true);
}

function focusPlayer(){
 const code=typeof game!=='undefined'?game.me:null;
 const item=countryLabels.find(x=>x.code===code);
 if(!item){reset();return}
 scale=clamp(minScale*3.1,minScale,Math.max(7.8,minScale*11));
 camX=item.x;camY=item.y;queueRender(true);
}

function ringAreaCentroid(ring){
 if(!ring||ring.length<3)return null;
 let a=0,cx=0,cy=0;
 for(let i=0,j=ring.length-1;i<ring.length;j=i++){
   const x0=ring[j][0],y0=ring[j][1],x1=ring[i][0],y1=ring[i][1];
   let dx=x1-x0;
   if(dx>180)x1-=360;else if(dx<-180)x1+=360;
   const cross=x0*y1-x1*y0;
   a+=cross;cx+=(x0+x1)*cross;cy+=(y0+y1)*cross;
 }
 a*=0.5;
 if(Math.abs(a)<1e-8){let sx=0,sy=0;for(const p of ring){sx+=p[0];sy+=p[1]}return{lon:sx/ring.length,lat:sy/ring.length,area:0}}
 cx/=6*a;cy/=6*a;
 while(cx>180)cx-=360;while(cx<-180)cx+=360;
 return{lon:cx,lat:cy,area:Math.abs(a)};
}
function representative(geom){
 if(!geom)return null;
 const polys=geom.type==='Polygon'?[geom.coordinates]:geom.type==='MultiPolygon'?geom.coordinates:[];
 let best=null;
 for(const poly of polys){const c=ringAreaCentroid(poly&&poly[0]);if(c&&(!best||c.area>best.area))best=c}
 if(!best)return null;
 return{x:(best.lon+180)*4,y:(90-best.lat)*4,area:best.area};
}
function countryCode(p){let c=(p.ADM0_A3||p.ISO_A3||p.SOV_A3||p.GU_A3||p.BRK_A3||p.POSTAL||'').toUpperCase();return c==='-99'?'':c}
function countryName(p){return isArabic()?(p.NAME_AR||p.ADMIN||p.NAME_EN||p.NAME||countryCode(p)):(p.ADMIN||p.NAME_EN||p.NAME||countryCode(p))}
function cityName(p){return isArabic()?(p.NAME_AR||p.NAME||''):(p.NAMEPAR||p.NAME||p.NAMEASCII||'')}

function buildLabelData(){
 countryLabels=[];cityLabels=[];
 const cf=(window.NE_COUNTRIES&&NE_COUNTRIES.features)||[];
 for(const f of cf){
   const p=f.properties||{},pos=representative(f.geometry),code=countryCode(p);if(!pos||!code)continue;
   countryLabels.push({code,name:countryName(p),x:pos.x,y:pos.y,area:pos.area,el:null});
 }
 countryLabels.sort((a,b)=>b.area-a.area);
 const cities=(window.NE_CITIES&&NE_CITIES.features)||[];
 for(const f of cities){
   const p=f.properties||{},g=f.geometry;if(!g||g.type!=='Point')continue;
   const n=cityName(p);if(!n)continue;
   const rank=Number(p.SCALERANK??p.scalerank??9);
   cityLabels.push({name:n,x:(g.coordinates[0]+180)*4,y:(90-g.coordinates[1])*4,rank:Number.isFinite(rank)?rank:9,el:null});
 }
 cityLabels.sort((a,b)=>a.rank-b.rank);
}

function makeLabel(item,cls){
 const d=document.createElement('div');d.className=cls;d.textContent=item.name;overlay.appendChild(d);item.el=d;return d;
}
function clearOverlay(){overlay.textContent='';for(const x of countryLabels)x.el=null;for(const x of cityLabels)x.el=null}

function collisionFree(occ,x,y,w,h){
 const cw=58,ch=22;
 const x0=Math.floor((x-w/2)/cw),x1=Math.floor((x+w/2)/cw),y0=Math.floor((y-h/2)/ch),y1=Math.floor((y+h/2)/ch);
 for(let gx=x0;gx<=x1;gx++)for(let gy=y0;gy<=y1;gy++)if(occ.has(gx+':'+gy))return false;
 for(let gx=x0;gx<=x1;gx++)for(let gy=y0;gy<=y1;gy++)occ.add(gx+':'+gy);
 return true;
}

function updateLabels(){
 if(!overlay||moving)return;
 clearOverlay();
 const vw=vp.clientWidth,vh=vp.clientHeight,t=cameraTransform(),r=zoomRatio(),occ=new Set();
 const maxCountries=r<1.35?42:r<1.9?68:r<2.8?110:r<4?170:260;
 let shown=0;
 for(const item of countryLabels){
   if(shown>=maxCountries&&item.code!==(game&&game.me))continue;
   const sx=t.tx+item.x*scale,sy=t.ty+item.y*scale;
   if(sx<-70||sx>vw+70||sy<-25||sy>vh+25)continue;
   const n=item.name||'',w=Math.max(42,Math.min(128,n.length*(isArabic()?6.6:5.8)+15));
   if(!collisionFree(occ,sx,sy,w,20)&&item.code!==(game&&game.me))continue;
   const el=makeLabel(item,'p4-country-label'+(r>3.6?' small':''));
   el.style.transform=`translate(${sx.toFixed(1)}px,${sy.toFixed(1)}px) translate(-50%,-50%)`;
   shown++;
 }
 if(r>=3.05){
   const rankLimit=r<4.1?1:r<5.7?2:r<7.5?3:5;
   let cityCount=0,maxCities=r<4.1?22:r<5.7?42:r<7.5?70:105;
   for(const item of cityLabels){
     if(cityCount>=maxCities)break;if(item.rank>rankLimit)continue;
     const sx=t.tx+item.x*scale,sy=t.ty+item.y*scale;
     if(sx<-80||sx>vw+80||sy<-30||sy>vh+30)continue;
     const w=Math.max(44,Math.min(120,item.name.length*(isArabic()?6.0:5.3)+18));
     if(!collisionFree(occ,sx,sy,w,18))continue;
     const el=makeLabel(item,'p4-city-label');el.style.transform=`translate(${sx.toFixed(1)}px,${sy.toFixed(1)}px) translate(-50%,-50%)`;cityCount++;
   }
 }
}

function installInput(){
 vp.addEventListener('touchstart',e=>{
   e.preventDefault();e.stopImmediatePropagation();setMoving(true);
   if(e.touches.length===1){
     const p=localTouch(e.touches[0]);single={x:p.x,y:p.y,startX:p.x,startY:p.y};pinch=null;tapMoved=false;tapTarget=e.target;
   }else if(e.touches.length>=2){
     const a=localTouch(e.touches[0]),b=localTouch(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2;
     const anchor=screenToWorld(mx,my);pinch={dist:Math.max(2,Math.hypot(a.x-b.x,a.y-b.y)),startScale:scale,anchorX:anchor.x,anchorY:anchor.y};single=null;tapMoved=true;tapTarget=null;
   }
 },{capture:true,passive:false});

 vp.addEventListener('touchmove',e=>{
   e.preventDefault();e.stopImmediatePropagation();
   if(e.touches.length===1&&single){
     const p=localTouch(e.touches[0]),dx=p.x-single.x,dy=p.y-single.y;
     if(Math.hypot(p.x-single.startX,p.y-single.startY)>6)tapMoved=true;
     camX-=dx/scale;camY-=dy/scale;single.x=p.x;single.y=p.y;queueRender(false);
   }else if(e.touches.length>=2&&pinch){
     tapMoved=true;
     const a=localTouch(e.touches[0]),b=localTouch(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2;
     const d=Math.max(2,Math.hypot(a.x-b.x,a.y-b.y));
     scale=clamp(pinch.startScale*d/pinch.dist,minScale,Math.max(7.8,minScale*11));
     camX=pinch.anchorX-(mx-vp.clientWidth/2)/scale;
     camY=pinch.anchorY-(my-vp.clientHeight/2)/scale;
     queueRender(false);
   }
 },{capture:true,passive:false});

 vp.addEventListener('touchend',e=>{
   e.preventDefault();e.stopImmediatePropagation();
   if(e.touches.length===0){
     if(!tapMoved&&tapTarget){const path=tapTarget.closest?tapTarget.closest('.p2country'):null;if(path&&window.p2SelectCountry)window.p2SelectCountry(path.dataset.code)}
     single=null;pinch=null;tapTarget=null;setMoving(false);
   }else if(e.touches.length===1){
     const p=localTouch(e.touches[0]);single={x:p.x,y:p.y,startX:p.x,startY:p.y};pinch=null;tapMoved=true;tapTarget=null;
   }
 },{capture:true,passive:false});

 vp.addEventListener('touchcancel',()=>{single=null;pinch=null;tapTarget=null;setMoving(false)},{capture:true,passive:false});
 vp.addEventListener('wheel',e=>{e.preventDefault();e.stopImmediatePropagation();const r=vp.getBoundingClientRect();setMoving(true);zoomAt(e.deltaY<0?1.12:.893,e.clientX-r.left,e.clientY-r.top);clearTimeout(labelTimer);labelTimer=setTimeout(()=>setMoving(false),90)},{capture:true,passive:false});
 vp.addEventListener('dblclick',e=>{e.preventDefault();e.stopImmediatePropagation();const r=vp.getBoundingClientRect();zoomAt(1.55,e.clientX-r.left,e.clientY-r.top)},{capture:true});
}

function wireTools(){
 const plus=document.getElementById('p2plus'),minus=document.getElementById('p2minus'),home=document.getElementById('p2home'),focus=document.getElementById('p2focus');
 if(plus)plus.onclick=()=>zoomAt(1.28,vp.clientWidth/2,vp.clientHeight/2);
 if(minus)minus.onclick=()=>zoomAt(.78,vp.clientWidth/2,vp.clientHeight/2);
 if(home)home.onclick=reset;
 if(focus)focus.onclick=focusPlayer;
}

function install(){
 const old=document.getElementById('viewport'),oldSvg=document.getElementById('world');
 if(!old||!oldSvg||!oldSvg.querySelector('.p2country')||!document.getElementById('p3layer')){setTimeout(install,100);return}
 injectStyle();
 const clone=old.cloneNode(true);old.parentNode.replaceChild(clone,old);
 vp=clone;wrap=clone.querySelector('#worldWrap');svg=clone.querySelector('#world');
 overlay=document.createElement('div');overlay.className='p4-overlay';overlay.id='p4overlay';vp.appendChild(overlay);
 let badge=document.getElementById('p4layer');if(!badge){badge=document.createElement('div');badge.id='p4layer';badge.className='p4-layer';document.getElementById('map').appendChild(badge)}
 let hint=document.getElementById('p4edgehint');if(!hint){hint=document.createElement('div');hint.id='p4edgehint';hint.className='p4-edgehint';hint.textContent=tr('اسحب الخريطة أو كبّر من موضع إصبعيك','Drag the map or pinch exactly where you want to zoom');document.getElementById('map').appendChild(hint)}
 buildLabelData();installInput();wireTools();
 window.resetMap=reset;window.applyTransform=function(){queueRender(true)};
 const prevStart=window.startGame;if(typeof prevStart==='function')window.startGame=function(){prevStart();setTimeout(reset,240)};
 const prevLang=window.setLang;if(typeof prevLang==='function')window.setLang=function(l){prevLang(l);setTimeout(()=>{buildLabelData();updateLabels()},180)};
 window.addEventListener('resize',()=>setTimeout(()=>{const oldMin=minScale;recalcMin();if(scale<=oldMin*1.08){scale=minScale;camX=WORLD_W/2;camY=WORLD_H/2}queueRender(true)},100));
 reset();hint.classList.add('show');setTimeout(()=>hint.classList.remove('show'),2600);
}

setTimeout(install,320);
})();
