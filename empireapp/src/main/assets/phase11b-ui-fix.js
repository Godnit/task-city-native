(function(){
'use strict';
const s=document.createElement('style');
s.id='p11bSafeTutorialStyle';
s.textContent=`
.p11Help{right:10px!important;top:258px!important;left:auto!important}
.ltr .p11Help{right:10px!important;left:auto!important}
/* Android WebView-safe tutorial: never use a huge shadow around the spotlight. */
.p11TourLayer,.p11TourLayer::before,.p11TourLayer::after{
  background:transparent!important;
  background-color:transparent!important;
  box-shadow:none!important;
  filter:none!important;
  backdrop-filter:none!important;
  pointer-events:none!important;
}
.p11Spot{
  background:transparent!important;
  background-color:transparent!important;
  box-shadow:0 0 0 3px rgba(155,217,206,.30),0 0 20px rgba(119,207,191,.70)!important;
  border:2px solid #9bd9ce!important;
  filter:none!important;
  backdrop-filter:none!important;
  pointer-events:none!important;
}
.p11Tip{
  z-index:302!important;
  opacity:1!important;
  visibility:visible!important;
  background:linear-gradient(#29433f,#172725)!important;
  border-color:#83a29b!important;
  box-shadow:0 12px 34px rgba(0,0,0,.66)!important;
  filter:none!important;
  backdrop-filter:none!important;
}
.p11Arrow{z-index:303!important;opacity:1!important}
#viewport{background:#2b7681!important}
#viewport,#worldWrap,#world,#p15Camera{visibility:visible!important;opacity:1!important;filter:none!important}
@media(max-height:480px){.p11Help{top:238px!important}}
`;
(document.head||document.documentElement).appendChild(s);

function keepMapVisible(){
  ['viewport','worldWrap','world','p15Camera'].forEach(id=>{
    const el=document.getElementById(id);
    if(!el)return;
    el.style.setProperty('visibility','visible','important');
    el.style.setProperty('opacity','1','important');
    el.style.setProperty('filter','none','important');
  });
}
keepMapVisible();
})();
