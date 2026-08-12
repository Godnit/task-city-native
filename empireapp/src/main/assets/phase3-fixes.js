(function(){
'use strict';
const W=1440,H=720;
let vp=null,wrap=null,svg=null,pinch=null,drag=false,lx=0,ly=0,labelRAF=0;
let countries=[],cities=[];

function ar(a,e){return typeof game!=='undefined'&&game.lang==='ar'?a:e}
function injectStyle(){
 if(document.getElementById('phase3MapFixStyle'))return;
 const s=document.createElement('style');s.id='phase3MapFixStyle';s.textContent=`
#world{--p3inv:1}
.p2label{font-size:calc(8.5px * var(--p3inv))!important;stroke-width:calc(1.65px * var(--p3inv))!important;letter-spacing:0!important}
.p2cityName{font-size:calc(6.6px * var(--p3inv))!important;stroke-width:calc(1.35px * var(--p3inv))!important;font-weight:600!important}
.p2hist{font-size:calc(9px * var(--p3inv))!important;stroke-width:calc(1.55px * var(--p3inv))!important}
.p2sea{font-size:calc(12px * var(--p3inv))!important;opacity:.2!important}
.p2cityDot{r:calc(1.65px * var(--p3inv));stroke-width:.65!important}
.p3layer{position:absolute;right:10px;top:137px;z-index:12;min-width:92px;text-align:center;background:#263d3add;border:1px solid #667c77;border-radius:5px;padding:5px 7px;color:#c8d6d3;font-size:10px;pointer-events:none}
`;
 document.head.appendChild(s);
}
function fitWidth(){return Math.max(.18,vp.clientWidth/W)}
function clamp(){
 const vw=vp.clientWidth,vh=vp.clientHeight,w=W*z,h=H*z;
 if(w<=vw+.5)px=(vw-w)/2;else px=Math.min(0,Math.max(vw-w,px));
 if(h<=vh+.5)py=(vh-h)/2;else py=Math.min(0,Math.max(vh-h,py));
}
function level(){let ratio=z/fitWidth();if(ratio<1.35)return 0;if(ratio<2.1)return 1;if(ratio<3.2)return 2;if(ratio<4.7)return 3;if(ratio<6.7)return 4;return 5}
function apply(){
 if(!vp||!wrap)return;
 clamp();
 wrap.style.transform=`translate3d(${px}px,${py}px,0) scale(${z})`;
 svg.style.setProperty('--p3inv',(1/Math.max(z,.001)).toFixed(5));
 if(window.zoomText)zoomText.textContent=Math.round(z*100)+'%';
 const b=document.getElementById('p3layer');if(b){let l=level();b.textContent=l<2?ar('الدول','Countries'):l<4?ar('الأقاليم','Provinces'):ar('المدن','Cities')}
 scheduleLabels();
}
function reset(){
 z=fitWidth();px=(vp.clientWidth-W*z)/2;py=(vp.clientHeight-H*z)/2;apply();
 if(window.regionCard)regionCard.classList.remove('show');
}
function zoomAt(f,cx,cy){
 const old=z,min=fitWidth(),nz=Math.max(min,Math.min(7.8,z*f));
 if(nz<=min*1.002){z=min;px=(vp.clientWidth-W*z)/2;py=(vp.clientHeight-H*z)/2;apply();return}
 const wx=(cx-px)/old,wy=(cy-py)/old;z=nz;px=cx-wx*nz;py=cy-wy*nz;apply();
}
function focusPlayer(){
 const p=svg.querySelector(`.p2country[data-code="${game.me}"]`);if(!p){reset();return}
 const b=p.getBBox(),cx=b.x+b.width/2,cy=b.y+b.height/2;z=Math.max(fitWidth(),Math.min(7.8,fitWidth()*3.2));px=vp.clientWidth/2-cx*z;py=vp.clientHeight/2-cy*z;apply();
}
function loc(t){const r=vp.getBoundingClientRect();return{x:t.clientX-r.left,y:t.clientY-r.top}}
function occupy(grid,x,y,w,h){
 const cw=52,ch=19,x0=Math.floor((x-w/2)/cw),x1=Math.floor((x+w/2)/cw),y0=Math.floor((y-h/2)/ch),y1=Math.floor((y+h/2)/ch);
 for(let gx=x0;gx<=x1;gx++)for(let gy=y0;gy<=y1;gy++)if(grid.has(gx+':'+gy))return false;
 for(let gx=x0;gx<=x1;gx++)for(let gy=y0;gy<=y1;gy++)grid.add(gx+':'+gy);return true;
}
function countryThreshold(l){return [35000,14000,5000,1500,350,80][l]||80}
function cityRankLimit(l){return [-1,-1,1,2,4,7][l]??7}
function updateLabels(){
 if(!vp)return;const vw=vp.clientWidth,vh=vp.clientHeight,l=level(),occ=new Set(),ct=countryThreshold(l);
 countries.forEach(o=>{
   const sx=px+o.x*z,sy=py+o.y*z,n=o.el.textContent||'';
   let show=o.area>=ct&&sx>-60&&sx<vw+60&&sy>-25&&sy<vh+25;
   if(show)show=occupy(occ,sx,sy,Math.max(38,Math.min(112,n.length*(game.lang==='ar'?5.8:5.1)+12)),17);
   o.el.style.opacity=show?'0.96':'0';o.el.style.pointerEvents='none';
 });
 const maxRank=cityRankLimit(l);
 cities.forEach(o=>{
   const sx=px+o.x*z,sy=py+o.y*z,n=o.name||'';
   let show=maxRank>=0&&o.rank<=maxRank&&sx>-55&&sx<vw+55&&sy>-20&&sy<vh+25;
   if(show)show=occupy(occ,sx,sy-6,Math.max(38,Math.min(118,n.length*(game.lang==='ar'?5.4:4.8)+14)),17);
   o.el.style.display=show?'block':'none';
 });
}
function scheduleLabels(){if(labelRAF)return;labelRAF=requestAnimationFrame(()=>{labelRAF=0;updateLabels()})}
function buildIndexes(){
 countries=[];svg.querySelectorAll('.p2label').forEach(el=>{const code=el.dataset.p2code,path=svg.querySelector(`.p2country[data-code="${code}"]`);if(!path)return;let b;try{b=path.getBBox()}catch(e){return}countries.push({el,x:+el.getAttribute('x')||b.x+b.width/2,y:+el.getAttribute('y')||b.y+b.height/2,area:b.width*b.height})});countries.sort((a,b)=>b.area-a.area);
 cities=[];const els=[...svg.querySelectorAll('.p2city')],features=(window.NE_CITIES&&NE_CITIES.features)||[];let j=0;
 for(const f of features){const p=f.properties||{},g=f.geometry;if(!g||g.type!=='Point')continue;const name=game.lang==='ar'?(p.NAME_AR||p.NAME||''):(p.NAMEPAR||p.NAME||p.NAMEASCII||'');if(!name)continue;const el=els[j++];if(!el)break;const dot=el.querySelector('.p2cityDot');if(!dot)continue;cities.push({el,x:+dot.getAttribute('cx'),y:+dot.getAttribute('cy'),rank:Number(p.SCALERANK??p.scalerank??9)||9,name})}
 cities.sort((a,b)=>a.rank-b.rank);
 scheduleLabels();
}
function installInput(){
 vp.addEventListener('touchstart',e=>{e.preventDefault();e.stopImmediatePropagation();if(e.touches.length===1){drag=true;const p=loc(e.touches[0]);lx=p.x;ly=p.y;pinch=null}else if(e.touches.length===2){drag=false;const a=loc(e.touches[0]),b=loc(e.touches[1]),cx=(a.x+b.x)/2,cy=(a.y+b.y)/2;pinch={dist:Math.hypot(a.x-b.x,a.y-b.y),startZ:z,wx:(cx-px)/z,wy:(cy-py)/z}}},{capture:true,passive:false});
 vp.addEventListener('touchmove',e=>{e.preventDefault();e.stopImmediatePropagation();if(e.touches.length===1&&drag){const p=loc(e.touches[0]);px+=p.x-lx;py+=p.y-ly;lx=p.x;ly=p.y;apply()}else if(e.touches.length===2&&pinch){const a=loc(e.touches[0]),b=loc(e.touches[1]),cx=(a.x+b.x)/2,cy=(a.y+b.y)/2,d=Math.hypot(a.x-b.x,a.y-b.y),nz=Math.max(fitWidth(),Math.min(7.8,pinch.startZ*d/pinch.dist));z=nz;px=cx-pinch.wx*nz;py=cy-pinch.wy*nz;apply()}},{capture:true,passive:false});
 vp.addEventListener('touchend',e=>{e.stopImmediatePropagation();if(e.touches.length===0){drag=false;pinch=null}else if(e.touches.length===1){drag=true;const p=loc(e.touches[0]);lx=p.x;ly=p.y;pinch=null}},{capture:true,passive:false});
 vp.addEventListener('wheel',e=>{e.preventDefault();e.stopImmediatePropagation();const r=vp.getBoundingClientRect();zoomAt(e.deltaY<0?1.13:.885,e.clientX-r.left,e.clientY-r.top)},{capture:true,passive:false});
 vp.addEventListener('dblclick',e=>{e.preventDefault();e.stopImmediatePropagation();const r=vp.getBoundingClientRect();zoomAt(1.55,e.clientX-r.left,e.clientY-r.top)},{capture:true});
}
function wireTools(){
 const plus=document.getElementById('p2plus'),minus=document.getElementById('p2minus'),home=document.getElementById('p2home'),focus=document.getElementById('p2focus');
 if(plus)plus.onclick=()=>zoomAt(1.28,vp.clientWidth/2,vp.clientHeight/2);
 if(minus)minus.onclick=()=>zoomAt(.78,vp.clientWidth/2,vp.clientHeight/2);
 if(home)home.onclick=reset;if(focus)focus.onclick=focusPlayer;
}
function install(){
 vp=document.getElementById('viewport');wrap=document.getElementById('worldWrap');svg=document.getElementById('world');if(!vp||!wrap||!svg||!svg.querySelector('.p2country')){setTimeout(install,80);return}
 injectStyle();
 let badge=document.getElementById('p3layer');if(!badge){badge=document.createElement('div');badge.id='p3layer';badge.className='p3layer';document.getElementById('map').appendChild(badge)}
 installInput();wireTools();buildIndexes();
 window.resetMap=reset;window.applyTransform=apply;
 const prevStart=window.startGame;if(typeof prevStart==='function')window.startGame=function(){prevStart();setTimeout(reset,130)};
 const prevLang=window.setLang;if(typeof prevLang==='function')window.setLang=function(l){prevLang(l);setTimeout(()=>{buildIndexes();apply()},60)};
 window.addEventListener('resize',()=>setTimeout(()=>{if(z<=fitWidth()*1.12)reset();else apply()},60));
 reset();
}
setTimeout(install,140);
})();
