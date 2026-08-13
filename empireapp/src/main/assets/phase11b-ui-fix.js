(function(){
'use strict';
const s=document.createElement('style');
s.id='p11bSafeTutorialStyle';
s.textContent=`
.p11Help{right:10px!important;top:258px!important;left:auto!important}
.ltr .p11Help{right:10px!important;left:auto!important}
/* The tutorial must highlight controls without making the world look blank. */
.p11TourLayer{background:transparent!important;pointer-events:none!important}
.p11Spot{
  background:transparent!important;
  box-shadow:0 0 0 9999px rgba(4,12,11,.20),0 0 0 4px rgba(119,207,191,.18),0 0 18px rgba(119,207,191,.42)!important;
  border:2px solid #9bd9ce!important;
}
.p11Tip{
  z-index:302!important;
  opacity:1!important;
  visibility:visible!important;
  background:linear-gradient(#29433f,#172725)!important;
  border-color:#83a29b!important;
  box-shadow:0 12px 34px rgba(0,0,0,.66)!important;
}
.p11Arrow{z-index:303!important;opacity:1!important}
#viewport,#worldWrap,#world,#p15Camera{visibility:visible!important;opacity:1!important}
@media(max-height:480px){.p11Help{top:238px!important}}
`;
(document.head||document.documentElement).appendChild(s);

function keepMapVisible(){
  ['viewport','worldWrap','world','p15Camera'].forEach(id=>{
    const el=document.getElementById(id);
    if(!el)return;
    el.style.setProperty('visibility','visible','important');
    el.style.setProperty('opacity','1','important');
  });
}
keepMapVisible();

// The old guided-tour spotlight intentionally used a nearly black 9999px shadow.
// On some Android WebViews it paints the whole map as a solid dark rectangle.
// Watch the tutorial state and immediately restore the map while keeping a light spotlight.
const root=document.body||document.documentElement;
new MutationObserver(()=>{
  const layer=document.getElementById('p11TourLayer');
  if(layer&&layer.style.display!=='none')keepMapVisible();
}).observe(root,{subtree:true,attributes:true,attributeFilter:['style','class']});
})();
