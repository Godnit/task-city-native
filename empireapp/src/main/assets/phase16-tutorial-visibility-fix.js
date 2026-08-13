(function(){
'use strict';
function install(){
  if(document.getElementById('p16TutorialFix')) return;
  const s=document.createElement('style');
  s.id='p16TutorialFix';
  s.textContent=`
/* Keep the map visible while the guided tutorial highlights controls. */
.p11TourLayer{background:transparent!important;pointer-events:none!important}
.p11Spot{
  background:transparent!important;
  box-shadow:0 0 0 9999px rgba(4,12,11,.24),0 0 0 4px rgba(119,207,191,.20),0 0 18px rgba(119,207,191,.48)!important;
  border:2px solid #9bd9ce!important;
  pointer-events:none!important;
}
.p11Tip{
  z-index:302!important;
  background:linear-gradient(180deg,#29433f 0%,#172725 100%)!important;
  border:1px solid #83a29b!important;
  box-shadow:0 12px 34px rgba(0,0,0,.68)!important;
  opacity:1!important;
  visibility:visible!important;
}
.p11Arrow{z-index:303!important;opacity:1!important}
#viewport,#worldWrap,#world,#p15Camera{
  visibility:visible!important;
  opacity:1!important;
}
`;
  (document.head||document.documentElement).appendChild(s);

  // Defensive guard: the tutorial may dim the screen, but it must never hide the actual map.
  const keepMapVisible=()=>{
    const vp=document.getElementById('viewport');
    const world=document.getElementById('world');
    const cam=document.getElementById('p15Camera');
    [vp,world,cam].forEach(el=>{
      if(!el) return;
      el.style.setProperty('visibility','visible','important');
      el.style.setProperty('opacity','1','important');
    });
  };
  keepMapVisible();
  const root=document.body||document.documentElement;
  new MutationObserver(()=>{
    const layer=document.getElementById('p11TourLayer');
    if(layer && layer.style.display!=='none') keepMapVisible();
  }).observe(root,{subtree:true,attributes:true,attributeFilter:['style','class']});
}
install();
})();
