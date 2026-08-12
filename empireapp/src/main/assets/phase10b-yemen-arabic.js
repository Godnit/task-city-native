(function(){
'use strict';
const gov={
 'Sanʿaʾ':'صنعاء','Sanʿaʾ Governorate':'أمانة العاصمة','Lahij Governorate':'لحج','‘Adan Governorate':'عدن','Al Hudaydah Governorate':'الحديدة',"Ta'izz Governorate":'تعز','Shabwah Governorate':'شبوة','Hadhramaut':'حضرموت','Abyan Governorate':'أبين','Al Jawf Governorate':'الجوف','Ibb Governorate':'إب',"Al Bayda' Governorate":'البيضاء',"Ad Dali' Governorate":'الضالع','Al Mahwit Governorate':'المحويت',"Sa'dah Governorate":'صعدة','Hajjah Governorate':'حجة','Dhamar Governorate':'ذمار',"'Amran Governorate":'عمران','Al Mahrah Governorate':'المهرة',"Ma'rib Governorate":'مأرب','Raymah Governorate':'ريمة','Socotra':'أرخبيل سقطرى'
};
const city={
 'Sanaa':'صنعاء','Sana’a':'صنعاء',"Sana'a":'صنعاء','Aden':'عدن','Taizz':'تعز','Taiz':'تعز','Al Hudaydah':'الحديدة','Hodeidah':'الحديدة','Al Mukalla':'المكلا','Mukalla':'المكلا','Ibb':'إب','Dhamar':'ذمار','Sadah':'صعدة',"Sa'dah":'صعدة','Marib':'مأرب',"Ma'rib":'مأرب','Hajjah':'حجة','Amran':'عمران','Ataq':'عتق','Sayun':'سيئون','Al Ghaydah':'الغيضة','Zinjibar':'زنجبار','Jeddah':'جدة','Jiddah':'جدة','Makkah':'مكة','Mecca':'مكة','Riyadh':'الرياض','Medina':'المدينة المنورة','Al Madinah':'المدينة المنورة','Khartoum':'الخرطوم','Port Sudan':'بورتسودان','Dongola':'دنقلا','Addis Ababa':'أديس أبابا','Djibouti':'جيبوتي','Asmara':'أسمرة','Berbera':'بربرة','Hargeisa':'هرجيسا','Muscat':'مسقط','Salalah':'صلالة','Doha':'الدوحة','Dubai':'دبي','Abu Dhabi':'أبوظبي','Manama':'المنامة'
};
function ar(){return typeof game!=='undefined'&&game.lang==='ar'}
function apply(){document.querySelectorAll('#p10YemLabels text').forEach(t=>{const en=t.dataset.en||t.textContent||'';if(!t.dataset.en)t.dataset.en=en;t.textContent=ar()?(gov[en]||en):en});document.querySelectorAll('.p2cityName').forEach(t=>{const en=t.dataset.p10en||t.dataset.p10ben||t.textContent||'';if(!t.dataset.p10ben)t.dataset.p10ben=en;t.textContent=ar()?(city[en]||en):en})}
function patch(){const old=window.setLang;if(typeof old==='function'&&!old.__p10b){const fn=function(l){const r=old(l);setTimeout(apply,120);return r};fn.__p10b=true;window.setLang=fn}}
function install(){apply();patch();const z=document.getElementById('zoomText');if(z)new MutationObserver(()=>requestAnimationFrame(apply)).observe(z,{childList:true,subtree:true,characterData:true});setTimeout(apply,500)}
setTimeout(install,450);
})();