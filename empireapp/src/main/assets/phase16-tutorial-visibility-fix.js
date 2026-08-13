(function(){
'use strict';
if(document.getElementById('p16TutorialFix')) return;
const s=document.createElement('style');
s.id='p16TutorialFix';
s.textContent=`
.p11TourLayer{background:transparent!important;background-color:transparent!important;box-shadow:none!important;filter:none!important;backdrop-filter:none!important;pointer-events:none!important}
.p11TourLayer::before,.p11TourLayer::after{display:none!important}
.p11Spot{background:transparent!important;background-color:transparent!important;box-shadow:0 0 0 3px rgba(155,217,206,.28),0 0 20px rgba(119,207,191,.72)!important;border:2px solid #9bd9ce!important;filter:none!important;backdrop-filter:none!important;pointer-events:none!important}
.p11Tip{z-index:302!important;opacity:1!important;visibility:visible!important;background:linear-gradient(180deg,#29433f 0%,#172725 100%)!important;border:1px solid #83a29b!important;box-shadow:0 12px 34px rgba(0,0,0,.68)!important}
.p11Arrow{z-index:303!important;opacity:1!important}
#viewport,#worldWrap,#world,#p15Camera{visibility:visible!important;opacity:1!important;filter:none!important}
`;
(document.head||document.documentElement).appendChild(s);
})();
