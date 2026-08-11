// Network Academy v7 — foundations first + exact guided missions
(function(){
  const DATA=window.ACADEMY_DATA;
  if(!DATA||!Array.isArray(DATA.lessons))return;
  const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const MASK='255.255.255.0';

  const F={
    0:{title:'قبل العناوين: كيف يصبح جهازان شبكة؟',lead:'افصل بين ثلاث أفكار: الجهاز، والطريق الذي يصل الأجهزة، ثم العنوان الذي يحدد الوجهة. وجود IP بلا طريق لا يصنع اتصالًا.',steps:['الجهاز يدخل الشبكة من واجهة شبكة NIC.','نحتاج طريقًا فعليًا: Ethernet أو Wi‑Fi.','Switch يجمع الأجهزة السلكية داخل LAN، وAccess Point يدخل الأجهزة اللاسلكية.','بعد وجود الطريق نستخدم عناوين مثل IP لتحديد الجهاز المقصود.'],extra:'الشبكة قد تكون جهازين فقط داخل غرفة حتى لو كان الإنترنت مقطوعًا. الإنترنت شبكة أوسع، وليس شرطًا لوجود LAN.',q:'جهازان لديهما IP صحيح لكن لا يوجد كابل أو Wi‑Fi أو مسار بينهما. ماذا يحدث؟',o:['لن يتواصلا لأن الطريق غير موجود','سيتواصلا لأن IP وحده يكفي','يتحول أحدهما تلقائيًا إلى Router'],a:0},
    1:{title:'LAN وWLAN وWAN بدون خلط',lead:'هذه الكلمات تصف نطاق الشبكة أو طريقة الدخول إليها، وليست أسماء لأجهزة جديدة.',steps:['LAN = شبكة محلية في منزل أو مكتب أو معمل.','WLAN = دخول لاسلكي إلى شبكة محلية عبر Wi‑Fi وAccess Point.','WAN = ربط شبكات أوسع أو بعيدة.','Wi‑Fi ليس هو الإنترنت؛ قد تعمل LAN/WLAN بينما الإنترنت مقطوع.'],extra:'الهاتف على Wi‑Fi والطابعة على كابل يمكن أن يكونا داخل LAN المنزل نفسها.',q:'الهاتف على Wi‑Fi والطابعة على كابل في المنزل والإنترنت مقطوع. ما الصحيح؟',o:['قد تبقى LAN تعمل بين الأجهزة','لا توجد أي شبكة إطلاقًا','الهاتف يصبح WAN'],a:0},
    2:{title:'من يفعل ماذا؟',lead:'اربط كل جهاز بوظيفة واحدة بدل حفظ الأسماء: من يجمع الأجهزة؟ من يدخل Wi‑Fi؟ من يخرج إلى شبكة أخرى؟',steps:['NIC = باب الجهاز إلى الشبكة.','Switch = يجمع أجهزة LAN السلكية.','Access Point = يدخل الهاتف/اللابتوب لاسلكيًا إلى LAN.','Router = يربط شبكة IP بشبكة أخرى؛ لا تحتاج المرور به لكل اتصال محلي.'],extra:'في راوتر المنزل قد تجتمع Router + Switch + Access Point داخل صندوق واحد، لذلك يبدو كأنه جهاز واحد يفعل كل شيء.',q:'PC وطابعة في نفس LAN ومتصلان بالسويتش. ما الجهاز المسؤول أساسًا عن الربط المحلي؟',o:['Switch','Router إلى الإنترنت حتمًا','DNS Server'],a:0},
    3:{title:'IPv4: لماذا نحتاج عنوانًا أصلًا؟',lead:'بعد أن أصبح لدينا طريق بين الأجهزة، نحتاج أن نحدد من المرسل ومن الجهاز المقصود. IP هو عنوان منطقي؛ ليس كابلًا ولا اتصالًا.',steps:['PC‑1 عنوانه مثل 192.168.1.10.','PC‑2 يجب أن يملك عنوانًا مختلفًا مثل 192.168.1.20.','Source IP = عنوان المرسل. Destination IP = عنوان الجهاز الذي نريد الوصول إليه.','Switch جزء من الطريق داخل LAN، لكنه ليس عنوان PC‑2. القناع في الدرس التالي يحدد هل الهدف محلي أم بعيد.'],extra:'Address تعني «عنوان» بشكل عام. IPv4 Address نوع من العناوين. لاحقًا سترى MAC Address وهو عنوان آخر لغرض مختلف.',q:'PC‑1 يريد إرسال حزمة إلى PC‑2. لماذا يحتاج عنوان IP الخاص بـPC‑2؟',o:['لتحديد الوجهة المنطقية للحزمة','لإنشاء الكابل','لتحويل PC‑2 إلى Switch'],a:0},
    4:{title:'Subnet Mask: هل الهدف داخل شبكتي؟',lead:'IP يخبرك بالعنوان، لكن Mask يخبر الجهاز أين تنتهي شبكته المحلية. لذلك نقرأ IP وMask معًا.',steps:['192.168.1.10/24 و192.168.1.20/24 في الشبكة نفسها هنا.','/24 تعني في هذا المثال 255.255.255.0.','إذا أصبح الهدف 192.168.2.20/24 فهو في شبكة أخرى.','عند اختلاف الشبكة نحتاج Router وDefault Gateway للوصول إلى الهدف.'],extra:'لا تحفظ قاعدة «أول ثلاث خانات» على أنها قانون دائم؛ هي نتيجة القناع /24 فقط.',q:'PC‑1 = 192.168.1.10/24 والهدف = 192.168.2.20/24. ماذا يعرف PC‑1؟',o:['الهدف في شبكة أخرى ويحتاج Router/Gateway','الهدف محلي حتمًا','عنوان الهدف غير مهم'],a:0},
    5:{title:'Default Gateway: باب الخروج من LAN',lead:'Gateway ليست «عنوان الإنترنت». هي عنوان واجهة الراوتر القريبة منك داخل شبكتك، وترسل إليها الحزم التي وجهتها في شبكة أخرى.',steps:['الجهاز يستخدم Mask ليقرر هل الهدف محلي أم بعيد.','الهدف المحلي يذهب داخل LAN ولا يحتاج Gateway لهذا الاتصال.','الهدف البعيد يرسل أولًا إلى Gateway مثل 192.168.1.1.','Router يستلم الحزمة ثم يختار الطريق إلى الشبكة التالية.'],extra:'مثال: PC‑1 = 192.168.1.10/24 وGateway = 192.168.1.1. يجب أن تكون البوابة نفسها قابلة للوصول من LAN.',q:'PC يصل إلى الأجهزة المحلية لكنه لا يصل إلى شبكة أخرى. ما القيمة المنطقية التي تفحصها بعد IP وMask؟',o:['Default Gateway','اسم الجهاز','لون السويتش'],a:0}
  };

  Object.keys(F).forEach(k=>{const l=DATA.lessons[+k],f=F[k];if(l){l.q=f.q;l.opts=f.o;l.ans=f.a;}});

  const chain=[['الاتصال','كابل / Wi‑Fi'],['LAN','Switch / AP'],['IP','عنوان الجهاز'],['Mask','حدود الشبكة'],['Gateway','باب الخروج'],['Router','إلى شبكة أخرى']];
  function lessonBox(i){
    const f=F[i];if(!f)return'';
    let topo='';
    if(i===3)topo='<div class="v7Topo"><div><b>PC‑1</b><code>192.168.1.10</code></div><span>──</span><div><b>Switch</b><small>طريق داخل LAN</small></div><span>──</span><div><b>PC‑2</b><code>192.168.1.20</code></div></div><div class="v7Packet"><b>مثال الحزمة:</b> Source IP = 192.168.1.10 ← المرسل، Destination IP = 192.168.1.20 ← الجهاز المطلوب.</div>';
    if(i===5)topo='<div class="v7Topo"><div><b>PC‑1</b><code>192.168.1.10</code></div><span>→</span><div><b>Gateway</b><code>192.168.1.1</code></div><span>→</span><div><b>شبكة أخرى</b><small>Internet / LAN2</small></div></div>';
    return `<section class="v7Foundation"><div class="v7Chain">${chain.map((x,n)=>`<div class="v7ChainItem ${n===Math.min(i,5)?'active':''}"><b>${x[0]}</b><span>${x[1]}</span></div>`).join('')}</div><h2>${esc(f.title)}</h2><p class="v7Lead">${esc(f.lead)}</p>${topo}<div class="v7Steps">${f.steps.map((s,n)=>`<div><b>${n+1}</b><span>${esc(s)}</span></div>`).join('')}</div><div class="v7Remember"><b>اربطها بما قبلها:</b> ${esc(f.extra)}</div></section>`;
  }

  const oldOpen=window.openLesson;
  if(typeof oldOpen==='function')window.openLesson=function(i){oldOpen(i);setTimeout(()=>{const p=document.querySelector('.lessonPage');if(!p||!F[i]||p.querySelector('.v7Foundation'))return;const hero=p.querySelector('.lessonHeroV4');if(hero)hero.insertAdjacentHTML('afterend',lessonBox(i));else p.insertAdjacentHTML('afterbegin',lessonBox(i));},0)};

  function add(type,name,x,y,vals={}){LAB.add(type);const n=LAB.state.nodes[LAB.state.nodes.length-1];Object.assign(n,{name,x,y,...vals});return n}
  function finishSetup(msg){LAB.save();LAB.render();setTimeout(()=>{try{refreshLabSelectors?.()}catch(e){}},0);toast(msg||'جهزت نقطة البداية فقط — أنت تكمل المهمة الآن')}
  const oldPrepare=window.prepareLessonLab;
  window.prepareLessonLab=function(i){
    if(!F[i]){if(typeof oldPrepare==='function')oldPrepare(i);return}
    LAB.reset();
    if(i===0){add('pc','PC-1',140,300);add('switch','SW-1',315,150);add('pc','PC-2',500,300);return finishSetup('وضعت الأجهزة فقط. أنت ستوصلها الآن.');}
    if(i===1){add('pc','PC-Wired',120,300);add('switch','SW-1',300,150);add('ap','AP-1',480,150,{ssid:'AcademyNet',wifiPass:'12345678'});add('laptop','Laptop-WiFi',520,330,{ssid:'AcademyNet',wifiPass:'12345678'});return finishSetup('وضعت أجهزة LAN/WLAN بلا روابط.');}
    if(i===2){add('router','Router',100,120);add('switch','Switch',280,120);add('ap','AP',470,120,{ssid:'AcademyNet',wifiPass:'12345678'});add('phone','Phone',500,330,{ssid:'AcademyNet',wifiPass:'12345678'});return finishSetup('وضعت الأجهزة الأربعة. اربطها حسب وظيفة كل جهاز.');}
    if(i===3){const a=add('pc','PC-1',140,300,{ip:'0.0.0.0',mask:MASK}),sw=add('switch','SW-1',315,150),b=add('pc','PC-2',500,300,{ip:'0.0.0.0',mask:MASK});LAB.connect(a.id,sw.id);LAB.connect(b.id,sw.id);return finishSetup('التوصيل جاهز، لكن IP غير مضبوط. أنت ستكتب العنوانين.');}
    if(i===4){const a=add('pc','PC-1',140,300,{ip:'192.168.1.10',mask:MASK}),sw=add('switch','SW-1',315,150),b=add('pc','PC-2',500,300,{ip:'192.168.1.20',mask:MASK});LAB.connect(a.id,sw.id);LAB.connect(b.id,sw.id);return finishSetup('بدأنا بجهازين في الشبكة نفسها. أنت ستنقل PC-2 مؤقتًا إلى شبكة أخرى.');}
    if(i===5){LAB.demo();const pc=LAB.state.nodes.find(n=>n.name==='PC-1'||n.type==='pc');if(pc)pc.gateway='';return finishSetup('الشبكة جاهزة لكن Gateway في PC-1 فارغة. أنت ستضبطها.');}
  };

  const GUIDE={
    0:{t:'كوّن LAN قبل أن نستخدم IP',g:'أثبت أولًا أن الشبكة تحتاج أجهزة + طريق.',s:[['ابدأ من الأجهزة','اضغط «ضع الأجهزة المطلوبة فقط — بدون حل المهمة».'],['وصل PC‑1','اضغط «توصيل»، ثم PC‑1 ثم SW‑1.'],['وصل PC‑2','اضغط «توصيل»، ثم PC‑2 ثم SW‑1.'],['تحقق','اضغط «تحقق من المهمة».']],e:['تظهر ثلاثة أجهزة بلا خطوط.','يظهر خط PC‑1 ↔ SW‑1.','يظهر خط ثانٍ PC‑2 ↔ SW‑1.','يقبل المعمل المهمة لأن الطريق المحلي اكتمل.'],c:['الأجهزة وحدها ليست شبكة متصلة.','التوصيل الفيزيائي يسبق IP.','Switch يجمع أجهزة LAN.','فهمنا الطريق قبل العنونة.']},
    1:{t:'شاهد LAN وWLAN في رسم واحد',g:'ميّز بين السلكي واللاسلكي بدل خلط Wi‑Fi بالإنترنت.',s:[['ابدأ','اضغط زر البداية.'],['الجزء السلكي','فعّل «توصيل»: PC‑Wired ↔ SW‑1، ثم SW‑1 ↔ AP‑1.'],['الجزء اللاسلكي','اضغط Laptop‑WiFi. تأكد أن SSID = AcademyNet وكلمة المرور = 12345678، ثم اضغط «الاتصال بنقطة وصول».'],['تحقق','اضغط «تحقق من المهمة».']],e:['أربعة أجهزة بلا روابط.','PC وAP متصلان بالسويتش.','يظهر رابط Wi‑Fi للابتوب.','ترى LAN فيها جزء سلكي وجزء WLAN.'],c:['LAN ليست الإنترنت.','AP نفسه يدخل إلى LAN.','WLAN طريقة دخول لاسلكية.','السلكي واللاسلكي قد يكونان في LAN واحدة.']},
    2:{t:'اربط كل جهاز بوظيفته',g:'ابنِ السلسلة Router → Switch → AP → Phone.',s:[['ابدأ','اضغط زر البداية.'],['Router مع Switch','فعّل «توصيل»: Router ثم Switch.'],['Switch مع AP','فعّل «توصيل»: Switch ثم AP.'],['الهاتف مع Wi‑Fi','اضغط Phone، طابق SSID وكلمة المرور مع AP، ثم «الاتصال بنقطة وصول».']],e:['تظهر الأجهزة بلا روابط.','يظهر Router ↔ Switch.','يظهر Switch ↔ AP.','يظهر Phone متصلًا لاسلكيًا بـAP.'],c:['Router عند حدود الشبكات.','Switch يجمع LAN.','AP يربط Wi‑Fi بالـLAN.','الهاتف يدخل عبر AP لا عبر كابل.']},
    3:{t:'اضبط IPv4 بنفسك — لا توجد قيم جاهزة',g:'أنت الآن ستعطي كل جهاز عنوانه ثم تستخدمهما كمصدر ووجهة.',s:[['عنوان PC‑1','اضغط PC‑1. في IP اكتب 192.168.1.10. في Subnet Mask اكتب 255.255.255.0. اترك Gateway وDNS فارغتين ثم «حفظ».'],['عنوان PC‑2','اضغط PC‑2. في IP اكتب 192.168.1.20. استخدم نفس Mask ثم «حفظ».'],['اختر من يرسل ولمن','في «أدوات الشبكة»: المصدر PC‑1 — 192.168.1.10، والهدف PC‑2 — 192.168.1.20.'],['اختبر','اضغط Ping ثم «تحقق من المهمة».']],e:['تحت PC‑1 يظهر 192.168.1.10.','تحت PC‑2 يظهر 192.168.1.20.','المصدر والهدف يحملان عنوانين مختلفين.','تظهر رسالة نجاح داخل الشبكة المحلية.'],c:['IP = عنوان منطقي لهذا الجهاز.','كل جهاز يحتاج عنوانًا مختلفًا.','Source = من يرسل، Destination = لمن نرسل.','Ping يثبت أن الطريق + العناوين يعملان.'],x:'بعد النجاح فقط: اجعل PC‑2 مؤقتًا 192.168.1.10 مثل PC‑1 وشغّل «تشخيص الشبكة كاملة» لترى IP Conflict، ثم أعده إلى 192.168.1.20.'},
    4:{t:'شاهد قرار Subnet Mask بنفسك',g:'غيّر شبكة PC‑2 فقط وشاهد لماذا يفشل الاتصال بدون Router.',s:[['ابدأ','اضغط زر البداية؛ PC‑1 = 192.168.1.10/24 وPC‑2 = 192.168.1.20/24.'],['اختبر الحالة المحلية','اختر PC‑1 وPC‑2 في أدوات الشبكة واضغط Ping؛ يجب أن ينجح.'],['غيّر شبكة PC‑2','اضغط PC‑2 وغيّر IP فقط إلى 192.168.2.20 مع إبقاء Mask = 255.255.255.0، ثم احفظ واضغط Ping.'],['أعده','أعد IP في PC‑2 إلى 192.168.1.20 واضغط Ping ثم «تحقق من المهمة».']],e:['الحالة الأولى في نفس /24.','Ping ينجح.','Ping يفشل بعد نقل PC‑2 إلى 192.168.2.x؛ هذا الفشل مقصود.','Ping يعود للنجاح.'],c:['IP وMask يقرآن معًا.','نفس Network ID = اتصال محلي.','شبكة مختلفة = نحتاج Router/Gateway.','إعادة العنوان تعيد القرار المحلي.']},
    5:{t:'ضع Default Gateway بيدك',g:'اجعل PC‑1 يعرف باب الخروج ثم اختبر هدفًا بعيدًا.',s:[['ابدأ','اضغط زر البداية. افتح PC‑1 ولاحظ أن Gateway فارغة.'],['اكتب البوابة','في PC‑1 اكتب Gateway = 192.168.1.1 ثم «حفظ».'],['اختر الهدف البعيد','في أدوات الشبكة اختر PC‑1 مصدرًا وInternet هدفًا.'],['اختبر','اضغط Ping ثم «تحقق من المهمة».']],e:['PC‑1 بلا Gateway.','تظهر 192.168.1.1 كبوابة.','الهدف أصبح خارج LAN.','ينجح الوصول عبر Router/NAT.'],c:['Gateway لا نحتاجها للهدف المحلي.','هي عنوان Router داخل LAN.','الهدف البعيد يخرج من الشبكة.','النجاح يثبت IP + Mask + Gateway + Router.']}
  };

  function stepActions(title,why){
    const t=String(title||''),w=String(why||'');
    if(/أضف|جهّز|ضع .*جهاز|استخدم .*تدريب/.test(t))return[['اضغط زر البداية أولًا.','تأكد أن أسماء الأجهزة المطلوبة ظهرت في مساحة العمل.'],'تظهر الأجهزة المطلوبة فقط؛ لا تعتبرها حل المهمة.'];
    if(/وصل|وصّل|صِل|اربط/.test(t))return[['اضغط «توصيل».','اضغط الجهاز الأول ثم الجهاز الثاني المذكورين في الخطوة.','تأكد أن خطًا ظهر بينهما.'],'يظهر خط اتصال بين الجهازين.'];
    if(/IP|192\.|عنوان/.test(t)&&!/DNS/.test(t))return[['اضغط الجهاز المطلوب في الرسم.','غيّر خانة IP إلى القيمة المذكورة في الخطوة.','اضبط Subnet Mask إن كانت مطلوبة، ثم اضغط «حفظ».'],'يظهر IP الجديد تحت اسم الجهاز.'];
    if(/Gateway|بوابة/.test(t))return[['اضغط الجهاز العميل.','غيّر خانة Gateway إلى عنوان واجهة الراوتر داخل LAN.','اضغط «حفظ».'],'تظهر قيمة Gateway الجديدة في إعدادات الجهاز.'];
    if(/Ping/.test(t))return[['انزل إلى «أدوات الشبكة».','اختر المصدر والهدف.','اضغط Ping واقرأ «سجل الأحداث».'],'تظهر نتيجة نجاح أو سبب فشل محدد.'];
    if(/Traceroute|قفز/.test(t))return[['اختر المصدر والهدف في أدوات الشبكة.','اضغط Traceroute.','اقرأ القفزات بالترتيب وحدد آخر قفزة ناجحة.'],'يظهر مسار الحزمة أو نقطة توقفها.'];
    if(/DHCP|توزيع/.test(t))return[['افتح Router/Server وتأكد أن DHCP Server مفعّل.','افتح العميل وفعل DHCP Client إذا لزم.','اضغط زر DHCP أسفل المعمل.','افتح العميل ثانية واقرأ IP وGateway وDNS.'],'تمتلئ إعدادات العميل تلقائيًا.'];
    if(/DNS|site\.local|سجل/.test(t))return[['افتح Server وتأكد أن DNS Service تعمل والسجل site.local يشير إلى IP الخادم.','افتح العميل وتأكد أن DNS يحمل IP الخادم.','في أدوات الشبكة اكتب site.local واضغط DNS.'],'ترى site.local → عنوان IP.'];
    if(/HTTP|Web|الموقع|الخدمة/.test(t))return[['اختبر DNS أولًا.','اختبر Ping إلى الخادم.','افتح Server وتأكد أن HTTP Service تعمل.','اضغط HTTP في أدوات الشبكة.'],'تنجح خدمة الويب بعد نجاح الطريق والاسم والخدمة.'];
    if(/Wi.?Fi|SSID|لاسلك|نقطة وصول/.test(t))return[['افتح Access Point واقرأ SSID وكلمة المرور.','افتح الهاتف/اللابتوب واكتب القيم نفسها.','اضغط «الاتصال بنقطة وصول».'],'يظهر رابط Wi‑Fi في الرسم.'];
    if(/NAT/.test(t))return[['افتح Router.','فعّل NAT ثم «حفظ».','اختبر من جهاز داخلي إلى Internet.'],'يظهر أثر NAT عند الخروج في هذا السيناريو.'];
    if(/Route|Routing|مسار/.test(t))return[['افتح Router.','راجع IP كل Interface أولًا.','في Static Routes اكتب الشبكة البعيدة والـMask والـNext Hop، ثم احفظ.','أعد Ping/Traceroute.'],'يستطيع الراوتر اختيار الطريق إلى الشبكة البعيدة.'];
    if(/تشخيص|أصلح/.test(t))return[['اضغط «تشخيص الشبكة كاملة».','اقرأ أول ملاحظة فقط.','غيّر إعدادًا واحدًا متعلقًا بها، ثم أعد التشخيص.'],'تختفي الملاحظة التي أصلحت سببها.'];
    return[[t,w].filter(Boolean),'راقب نتيجة الخطوة في الرسم أو سجل الأحداث.'];
  }

  function decorateGuide(){
    const g=document.querySelector('.guidedLab');if(!g)return;
    const i=Number.isInteger(window._lessonLabIndex)?window._lessonLabIndex:null;
    if(g.dataset.v7===String(i))return;g.dataset.v7=String(i);
    g.querySelectorAll('.v6GuideIntro,.v6StepExplain,.v7GuideIntro,.v7NoAuto').forEach(x=>x.remove());
    const wrap=g.querySelector('.guideSteps');if(!wrap)return;
    wrap.insertAdjacentHTML('beforebegin','<div class="v7GuideIntro"><b>🎯 لا توجد أسئلة مكررة هنا</b><span>كل خطوة الآن تقول: أين تضغط، ماذا تغيّر، ماذا تكتب، ثم ما النتيجة التي يجب أن تراها.</span></div>');
    if(i!==null&&GUIDE[i]){
      const d=GUIDE[i],h=g.querySelector('h2'),p=g.querySelector(':scope > p');if(h)h.textContent=d.t;if(p)p.textContent=d.g;
      wrap.innerHTML=d.s.map((s,n)=>`<div class="guideStep v7Step"><b>${n+1}</b><div><strong>${esc(s[0])}</strong><div class="v7Do"><b>☝ افعل الآن</b><p>${esc(s[1])}</p></div><div class="v7See"><b>المفروض أن ترى:</b> ${esc(d.e[n])}</div><div class="v7Idea"><b>الفكرة التي أثبتتها:</b> ${esc(d.c[n])}</div>${d.x&&n===d.s.length-1?`<details class="v7Optional"><summary>تجربة إضافية بعد النجاح — ليست شرطًا</summary><p>${esc(d.x)}</p></details>`:''}</div></div>`).join('');
    }else{
      g.querySelectorAll('.guideStep').forEach(el=>{el.classList.add('v7Step');const body=el.querySelector('div'),t=el.querySelector('strong')?.textContent||'',why=el.querySelector('small')?.textContent||'';if(!body)return;el.querySelector('small')?.remove();const x=stepActions(t,why);body.insertAdjacentHTML('beforeend',`<div class="v7Do"><b>☝ افعل الآن</b><ol>${x[0].map(a=>`<li>${esc(a)}</li>`).join('')}</ol></div><div class="v7See"><b>المفروض أن ترى:</b> ${esc(x[1])}</div><div class="v7Idea"><b>الفكرة التي أثبتتها:</b> ${esc(why||'غيّر شيئًا واحدًا ثم اختبر أثره.')}</div>`);});
    }
    const b=g.querySelector('.guideBtns .primary');if(b)b.textContent='⚡ ضع الأجهزة المطلوبة فقط — بدون حل المهمة';
    const btns=g.querySelector('.guideBtns');if(btns)btns.insertAdjacentHTML('beforebegin','<div class="v7NoAuto"><b>مهم:</b> زر البداية لا يكتب IP أو Gateway بدلًا عنك في هذه الدروس؛ هو يضع نقطة البداية فقط.</div>');
  }

  function sameLink(a,b){return !!LAB.state.links.find(l=>(l.a===a&&l.b===b)||(l.a===b&&l.b===a))}
  const oldVerify=window.verifyLessonLab;
  window.verifyLessonLab=function(i){
    if(!F[i]){if(typeof oldVerify==='function')oldVerify(i);return}
    let ok=false,n=LAB.state.nodes;
    if(i===0){const p=n.filter(x=>x.type==='pc'),s=n.find(x=>x.type==='switch');ok=p.length>=2&&s&&p.every(x=>sameLink(x.id,s.id));}
    if(i===1){const pc=n.find(x=>x.name==='PC-Wired'),sw=n.find(x=>x.name==='SW-1'),ap=n.find(x=>x.name==='AP-1'),lap=n.find(x=>x.name==='Laptop-WiFi');ok=!!(pc&&sw&&ap&&lap&&sameLink(pc.id,sw.id)&&sameLink(sw.id,ap.id)&&sameLink(lap.id,ap.id));}
    if(i===2){const r=n.find(x=>x.type==='router'),sw=n.find(x=>x.type==='switch'),ap=n.find(x=>x.type==='ap'),ph=n.find(x=>x.type==='phone');ok=!!(r&&sw&&ap&&ph&&sameLink(r.id,sw.id)&&sameLink(sw.id,ap.id)&&sameLink(ph.id,ap.id));}
    if(i===3){const p=n.filter(x=>x.type==='pc');ok=p.length>=2&&p.some(x=>x.ip==='192.168.1.10')&&p.some(x=>x.ip==='192.168.1.20')&&p.every(x=>x.mask===MASK)&&!!LAB.canPing(p[0].id,p[1].id).ok;}
    if(i===4){const p=n.filter(x=>x.type==='pc');ok=p.length>=2&&p.some(x=>x.ip==='192.168.1.10')&&p.some(x=>x.ip==='192.168.1.20')&&!!LAB.canPing(p[0].id,p[1].id).ok;}
    if(i===5){const pc=n.find(x=>x.name==='PC-1'||x.type==='pc'),internet=n.find(x=>x.type==='internet');ok=!!(pc&&internet&&pc.gateway==='192.168.1.1'&&LAB.canPing(pc.id,internet.id).ok);}
    const box=document.getElementById('guideResult');
    if(ok){progress.labs=Array.isArray(progress.labs)?progress.labs:[];if(!progress.labs.includes(i)){progress.labs.push(i);saveProgress()}if(box)box.innerHTML='<div class="guideSuccess">✓ نجحت لأنك نفذت الإعدادات بنفسك. حاول الآن شرح السبب بجملة واحدة قبل العودة للدرس.</div>';toast('✅ نجحت المهمة');}
    else{if(box)box.innerHTML='<div class="guideFail">المهمة لم تكتمل. لا تغيّر كل شيء: ارجع إلى آخر خطوة، نفذ القيمة/التوصيل المكتوب فيها حرفيًا، ثم تحقق مرة أخرى.</div>';toast('بقي شرط في المهمة — راجع آخر خطوة');}
  };

  function settingsHelp(){
    const m=document.querySelector('#modalRoot .modal');if(!m||m.dataset.v7==='1')return;m.dataset.v7='1';m.querySelectorAll('.v6FieldWhy').forEach(x=>x.remove());
    const h=m.querySelector('h2');if(h)h.insertAdjacentHTML('afterend','<div class="v7Settings"><b>🧠 معنى الخانات قبل أن تكتب</b><p><strong>IP</strong> = عنوان هذا الجهاز. <strong>Mask</strong> = حدود الشبكة المحلية. <strong>Gateway</strong> = عنوان الراوتر للخروج. <strong>DNS</strong> = عنوان خادم الأسماء.</p></div>');
    const map={'IP':['IP — عنوان هذا الجهاز','مثال: 192.168.1.10. هذا يعرّف الجهاز منطقيًا.'],'Subnet Mask':['Subnet Mask — حدود الشبكة','مثال: 255.255.255.0. به يعرف الجهاز هل الهدف محلي.'],'Gateway':['Gateway — باب الخروج','عادة IP واجهة Router داخل LAN، مثل 192.168.1.1.'],'DNS':['DNS — خادم الأسماء','الخادم الذي يحول site.local أو اسم موقع إلى IP.'],'SSID':['SSID — اسم Wi‑Fi','اسم الشبكة اللاسلكية المطلوب الانضمام إليها.'],'كلمة المرور':['كلمة المرور — إذن Wi‑Fi','يجب أن تطابق إعداد Access Point.']};
    m.querySelectorAll('.field label').forEach(l=>{const x=map[l.textContent.trim()];if(!x)return;l.textContent=x[0];l.insertAdjacentHTML('afterend',`<small class="v7Field">${esc(x[1])}</small>`);});
  }

  const screen=document.getElementById('screen');if(screen)new MutationObserver(()=>setTimeout(decorateGuide,0)).observe(screen,{childList:true,subtree:true});
  const mr=document.getElementById('modalRoot');if(mr)new MutationObserver(()=>setTimeout(settingsHelp,0)).observe(mr,{childList:true,subtree:true});
  setTimeout(()=>{decorateGuide();settingsHelp();},0);
})();
