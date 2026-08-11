// Network Academy v6 — causal explanations for every guided lab step
(function(){
  const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

  function explainStep(title,why){
    const t=(title||'').toLowerCase(), w=(why||'').replace(/^لماذا؟\s*/,'').trim();
    let x={why:w||'هذه الخطوة تجهز جزءًا ضروريًا من مسار الاتصال قبل الانتقال للخطوة التالية.',
      happens:'يتغير جزء محدد من حالة الشبكة، وبذلك يصبح الاختبار التالي قادرًا على قياس نتيجة هذه الخطوة بدل التخمين.',
      skip:'قد تبدو الشبكة مكتملة بصريًا، لكن واحدًا من الشروط المنطقية أو الفيزيائية سيبقى ناقصًا وتفشل النتيجة.',
      verify:'قارن حالة الجهاز أو الوصلة قبل الخطوة وبعدها، ثم استخدم أداة الاختبار المناسبة للتأكد.',
      real:'في الشبكات الحقيقية ننفذ التغيير ثم نختبر أثره مباشرة، ولا نعدّ الإعداد صحيحًا لمجرد أنه مكتوب.'};

    if(/أضف|جه[ّز]|استخدم.*تدريب|ضع .*switch|ضع .*router/.test(t)){
      x.happens='تظهر المكوّنات التي ستشارك في التجربة، فيصبح لديك مخطط واضح يمكن توصيله وضبطه واختباره.';
      x.skip='إذا كان جهاز أساسي مفقودًا فلن تستطيع محاكاة المسار الذي يشرحه الدرس، حتى لو كانت بقية الإعدادات صحيحة.';
      x.verify='تأكد أن كل جهاز مذكور في الخطوة ظاهر في مساحة العمل وبالنوع الصحيح.';
      x.real='المهندس يبدأ بتحديد الأجهزة المطلوبة قبل مد الكابلات، مثل تحديد الراوتر والسويتش ونقطة الوصول قبل تركيب شبكة مكتب.';
    }
    if(/وص[ّ]?ل|صِل|اربط|وصلة|الاتصال بنقطة وصول/.test(t)){
      x.happens='تُنشأ وصلة يمكن أن تمر عبرها الإطارات أو الحزم. من دون مسار اتصال لا تستطيع البيانات الوصول حتى لو كانت عناوين IP صحيحة.';
      x.skip='يبقى الجهاز معزولًا، وغالبًا يظهر الفشل على أنه عدم وصول أو جهاز غير متصل.';
      x.verify='يجب أن ترى خط الاتصال بين الأجهزة، ثم جرّب Ping بعد اكتمال بقية الإعدادات.';
      x.real='هذا يشبه توصيل الحاسوب بمنفذ الشبكة في الجدار أو ربط الهاتف بنقطة Wi‑Fi قبل توقع أي اتصال.';
    }
    if(/ssid|كلمة المرور|wi.?fi|لاسلك/.test(t)){
      x.happens='يحاول العميل الارتباط بالشبكة اللاسلكية ذات الهوية الصحيحة. SSID يحدد الشبكة، وكلمة المرور تثبت السماح بالانضمام.';
      x.skip='إذا اختلف الاسم أو كلمة المرور فلن تتكوّن وصلة Wi‑Fi أصلًا، لذلك لن يصل العميل لاحقًا إلى DHCP أو الإنترنت.';
      x.verify='بعد النجاح يجب أن تظهر وصلة Wi‑Fi في الرسم، ثم يمكن للعميل طلب IP.';
      x.real='كما يحدث عندما تختار شبكة المنزل من الهاتف وتدخل كلمة المرور؛ القرب من الراوتر وحده لا يكفي.';
    }
    if(/ip|192\.|10\.0|عنوان/.test(t) && !/dns/.test(t)){
      x.happens='يحصل الجهاز أو الواجهة على هوية منطقية داخل شبكة IP، وبها يعرف الآخرون إلى أين يرسلون الرد.';
      x.skip='العنوان 0.0.0.0 أو عنوان مكرر أو من شبكة خاطئة يمنع التواصل الصحيح وقد يسبب IP Conflict.';
      x.verify='تأكد أن العنوان صالح وغير مكرر، ثم افحص هل الأجهزة المقصودة تقع في الشبكة نفسها أو تحتاج راوترًا.';
      x.real='مثل عنوان المنزل: وجود الطريق لا يكفي إذا لم يكن لكل منزل عنوان مختلف يمكن الوصول إليه.';
    }
    if(/mask|قناع|subnet|\/24/.test(t)){
      x.happens='يستخدم الجهاز القناع ليقرر: هل الهدف محلي فأرسله مباشرة، أم بعيد فأرسله إلى Default Gateway؟';
      x.skip='قناع خاطئ قد يجعل الجهاز يظن أن هدفًا بعيدًا محلي أو العكس، فتتجه الحزمة إلى المكان الخطأ.';
      x.verify='قارن Network ID للجهازين؛ ومع /24 يجب أن تتطابق أول ثلاث خانات كي يكونا عادة في الشبكة نفسها.';
      x.real='القناع يشبه حدود الحي: يحدد أي العناوين تعد داخل منطقتك وأيها تحتاج الخروج عبر بوابة.';
    }
    if(/gateway|بوابة/.test(t)){
      x.happens='يحفظ الجهاز عنوان الراوتر الذي يستقبل الحزم المتجهة إلى شبكات أخرى.';
      x.skip='قد يعمل الاتصال داخل LAN لكن يفشل أي هدف خارجها، لأن الجهاز لا يعرف أين يرسل الحركة البعيدة.';
      x.verify='يجب أن يكون Gateway عنوان واجهة الراوتر الموجودة في نفس subnet الخاصة بالجهاز، ثم اختبر هدفًا بعيدًا.';
      x.real='مثل بوابة الحي: تتحرك داخله مباشرة، لكن للخروج إلى مدينة أخرى تمر من مخرج معروف.';
    }
    if(/dhcp|توزيع/.test(t)){
      x.happens='يطلب العميل إعداداته تلقائيًا، فيحصل عادة على IP وMask وGateway وDNS من الخادم.';
      x.skip='إذا لم يصل العميل إلى DHCP يبقى بلا عنوان صالح أو يحصل في هذه المحاكاة على APIPA من 169.254.x.x.';
      x.verify='بعد التوزيع افتح العميل وتأكد أن IP وGateway وDNS امتلأت بقيم من النطاق المتوقع.';
      x.real='هذه هي الطريقة المعتادة التي يحصل بها هاتفك على إعدادات الشبكة فور اتصاله براوتر المنزل.';
    }
    if(/dns|site\.local|سجل/.test(t)){
      x.happens='يتحول الاسم الذي يفهمه الإنسان إلى عنوان IP يمكن للشبكة أن ترسل إليه الحزم.';
      x.skip='قد ينجح Ping إلى IP بينما يفشل فتح الاسم، لأن الطريق يعمل لكن ترجمة الاسم لا تعمل.';
      x.verify='استخدم أداة DNS وتأكد أن الاسم يرجع إلى IP الخادم المقصود.';
      x.real='عند كتابة اسم موقع، جهازك يحتاج DNS لمعرفة عنوان الخادم قبل بدء الاتصال بالخدمة.';
    }
    if(/http|web|الموقع|الخدمة/.test(t)){
      x.happens='بعد نجاح الشبكة وDNS، تختبر طبقة التطبيق نفسها: هل خدمة الويب تعمل وتستجيب؟';
      x.skip='قد تصل إلى الجهاز بنجاح لكن يبقى الموقع غير متاح إذا كانت HTTP Service متوقفة.';
      x.verify='اختبر DNS ثم Ping ثم HTTP بالترتيب؛ نجاح HTTP يعني أن السلسلة وصلت حتى الخدمة.';
      x.real='الوصول إلى مبنى الشركة لا يعني أن المكتب المطلوب مفتوح؛ الشبكة قد تعمل والخدمة نفسها متوقفة.';
    }
    if(/nat|private|internet/.test(t)){
      x.happens='يترجم الراوتر حركة العناوين الخاصة عند خروجها إلى الشبكة الخارجية في هذا السيناريو.';
      x.skip='قد يوجد Route إلى الخارج لكن الاتصال المحاكى يفشل لأن العنوان الخاص لم يُترجم كما تتطلب التجربة.';
      x.verify='فعّل NAT ثم اختبر من جهاز داخلي إلى Internet، وبعدها عطله مؤقتًا لترى الفرق.';
      x.real='راوتر المنزل يسمح لعدة أجهزة خاصة بمشاركة اتصال خارجي بدل إعطاء كل جهاز عنوانًا عامًا مستقلًا.';
    }
    if(/interface|واجهة|route|routing|مسار|راوتر/.test(t)){
      x.happens='يعرف الراوتر الشبكة الموجودة على كل واجهة وإلى أين يرسل الحزم للشبكات غير المتصلة به مباشرة.';
      x.skip='قد تصل الحزمة إلى راوتر ثم تتوقف، أو يصل الطلب ولا يعرف الرد طريق العودة.';
      x.verify='افحص IP كل Interface والـRoutes في الاتجاهين، ثم استخدم Traceroute لمعرفة آخر قفزة ناجحة.';
      x.real='كل تقاطع طرق يحتاج معرفة الطريق التالي؛ ومع تعدد الراوترات يجب أن توجد خريطة للذهاب والعودة.';
    }
    if(/ping/.test(t)){
      x.happens='يرسل المصدر طلب ICMP Echo ويتوقع ردًا، وبذلك يختبر إمكانية الوصول الأساسية بين نقطتين.';
      x.skip='ستبقى لا تعرف هل المشكلة في المسار أم في خدمة أعلى مثل DNS أو HTTP.';
      x.verify='نجاح Ping يثبت الوصول الأساسي فقط؛ بعده اختبر الخدمة المطلوبة بشكل مستقل.';
      x.real='مثل طرق الباب للتأكد أن العنوان قابل للوصول، لكنه لا يثبت أن كل خدمة داخل الجهاز تعمل.';
    }
    if(/traceroute|قفز/.test(t)){
      x.happens='يعرض المعمل الأجهزة أو الراوترات التي مرت بها الحزمة بالترتيب، فتستطيع تحديد أين يتوقف الطريق.';
      x.skip='عند الفشل ستعرف فقط أن الهدف لم يصل، لكن لن تعرف أي جزء من الطريق يحتاج الفحص أولًا.';
      x.verify='قارن قائمة القفزات بالرسم وحدد آخر جهاز ظهر قبل نقطة التوقف.';
      x.real='يشبه تتبع شحنة محطة بعد محطة لمعرفة في أي مدينة انقطع المسار.';
    }
    if(/تشخيص|أصلح|راجع.*خطأ|ملاحظة/.test(t)){
      x.happens='يفحص المعمل عدة طبقات ويحول الأعراض إلى قائمة أسباب محتملة بدل تغيير الإعدادات عشوائيًا.';
      x.skip='قد تصلح شيئًا غير متعلق بالمشكلة أو تنشئ عطلًا جديدًا، لأنك لم تحدد موضع الخلل أولًا.';
      x.verify='غيّر سببًا واحدًا فقط، ثم أعد نفس الاختبار لترى هل اختفت الملاحظة قبل الانتقال لغيرها.';
      x.real='هذا هو أسلوب Troubleshooting: عرض → فرضية → اختبار → تغيير واحد → إعادة اختبار.';
    }
    if(/غي[ّ]?ر|عط[ّ]?ل|افصل|جر[ّ]?ب مؤقت/.test(t)){
      x.happens='أنت تصنع خطأً متعمدًا لترى العلاقة بين السبب والنتيجة بدل حفظ القاعدة نظريًا.';
      x.skip='ستعرف الإعداد الصحيح، لكن قد لا تفهم لماذا هو صحيح أو كيف يبدو العطل عندما يتغير.';
      x.verify='قارن نتيجة الاختبار قبل التغيير وبعده، ثم أعد القيمة الصحيحة وتأكد أن النجاح عاد.';
      x.real='في التدريب الآمن نكسر إعدادًا واحدًا عمدًا كي نتعلم تشخيصه عندما يحدث في شبكة حقيقية.';
    }
    return x;
  }

  function enrichGuidedLab(){
    const guide=document.querySelector('.guidedLab');
    if(!guide || guide.dataset.v6==='1') return;
    guide.dataset.v6='1';
    const head=document.createElement('div');
    head.className='v6GuideIntro';
    head.innerHTML='<b>🧠 لا تنفذ الخطوات كحفظ</b><span>في كل خطوة ستعرف السبب، ما الذي يحدث داخل الشبكة، ماذا يحدث لو تركتها، وكيف تتأكد أنك نفذتها بشكل صحيح.</span>';
    const steps=guide.querySelector('.guideSteps');
    if(steps) guide.insertBefore(head,steps);
    guide.querySelectorAll('.guideStep').forEach((el,idx)=>{
      const title=el.querySelector('strong')?.textContent?.trim()||`الخطوة ${idx+1}`;
      const whyText=el.querySelector('small')?.textContent?.trim()||'';
      const x=explainStep(title,whyText);
      const oldSmall=el.querySelector('small'); if(oldSmall) oldSmall.remove();
      const body=el.querySelector('div'); if(!body) return;
      const details=document.createElement('div');
      details.className='v6StepExplain';
      details.innerHTML=`
        <div class="v6Why"><b>لماذا نفعلها؟</b><span>${esc(x.why)}</span></div>
        <details open><summary>ماذا يحدث داخل الشبكة؟</summary><p>${esc(x.happens)}</p></details>
        <details><summary>ماذا لو لم أفعلها أو فعلتها خطأ؟</summary><p>${esc(x.skip)}</p></details>
        <details><summary>كيف أعرف أن الخطوة نجحت؟</summary><p>${esc(x.verify)}</p></details>
        <details><summary>مثال من الواقع</summary><p>${esc(x.real)}</p></details>`;
      body.appendChild(details);
    });
  }

  function explainStatusText(text){
    const s=String(text||'');
    if(!s.trim())return null;
    if(/IP Conflict/.test(s))return ['تعارض عناوين IP','هناك جهازان يستخدمان العنوان نفسه، لذلك لا تستطيع الشبكة تحديد من يملك هذا العنوان بثقة. غيّر أحد العنوانين ثم أعد الاختبار.'];
    if(/APIPA|169\.254/.test(s))return ['العميل لم يصل إلى DHCP','عنوان 169.254.x.x يعني في هذا المعمل أن العميل لم يحصل على إعداداته من DHCP. افحص التوصيل، ثم وجود DHCP Server، ثم أعد التوزيع.'];
    if(/لا يوجد IP|0\.0\.0\.0|بلا IP/.test(s))return ['لا توجد هوية IP صالحة','الجهاز متصل ربما، لكنه لا يملك عنوانًا منطقيًا صالحًا للتواصل. اضبط IP يدويًا أو فعّل DHCP حسب التجربة.'];
    if(/غير متصل|لا توجد واجهات متصلة/.test(s))return ['المسار الفيزيائي ناقص','قبل IP وDNS يجب أن توجد وصلة فعلية أو لاسلكية. افحص الكابل/الرابط أولًا ثم انتقل للإعدادات.'];
    if(/Gateway|البوابة|بوابة/.test(s))return ['مشكلة في Default Gateway','الجهاز لا يعرف البوابة الصحيحة للشبكات البعيدة. يجب أن تكون البوابة واجهة راوتر في نفس subnet الخاصة بالجهاز.'];
    if(/Route|route|مسار|توقف/.test(s) && /Traceroute|راوتر|شبكة/.test(s))return ['المسار لا يكتمل','هناك نقطة في الطريق لا تعرف الشبكة التالية أو طريق العودة. استخدم Traceroute وافحص Routes وواجهات الراوترات.'];
    if(/DNS/.test(s) && /❌|لا يوجد|متوقفة|لا يمكن/.test(s))return ['مشكلة DNS','اختبر أولًا الوصول إلى IP خادم DNS. إذا كان الوصول يعمل، افحص عنوان DNS على العميل والخدمة والسجل Name → IP.'];
    if(/HTTP Service متوقفة/.test(s))return ['الشبكة تعمل لكن خدمة الويب متوقفة','هذه نتيجة مهمة: الوصول إلى الخادم نجح، إذن لا تعد إلى الكابل أو IP؛ فعّل HTTP Service نفسها.'];
    if(/لا يمكن الوصول/.test(s))return ['فشل الوصول قبل الخدمة','لا تبدأ بإصلاح HTTP أو DNS قبل التأكد من الرابط وIP/Mask/Gateway والمسار. شغّل Ping ثم Traceroute لتضييق موضع العطل.'];
    if(/✅/.test(s) && /Ping/.test(s))return ['نجاح الوصول الأساسي','Ping نجح، وهذا يثبت وجود مسار IP ورد أساسي. لا يعني ذلك تلقائيًا أن DNS أو HTTP يعملان؛ اختبرهما إذا كانت المهمة تحتاجهما.'];
    if(/✅/.test(s) && /DHCP/.test(s))return ['نجح الإعداد التلقائي','العميل وصل إلى DHCP وحصل على إعدادات. افتح الجهاز وتأكد من IP وGateway وDNS بدل الاكتفاء برسالة النجاح.'];
    if(/✅/.test(s) && /→/.test(s))return ['نجح حل الاسم','DNS أعاد IP للاسم. الخطوة التالية في تطبيق ويب هي التأكد أن الخادم قابل للوصول وأن HTTP تعمل.'];
    if(/✅/.test(s))return ['الاختبار نجح','اسأل نفسك: ما الطبقة التي أثبتها هذا الاختبار تحديدًا؟ النجاح في اختبار واحد لا يثبت كل خدمات الشبكة.'];
    return ['فسّر النتيجة قبل التعديل','اقرأ الرسالة كدليل على موضع المشكلة، ثم غيّر إعدادًا واحدًا فقط وأعد نفس الاختبار حتى تعرف سبب التحسن.'];
  }

  function attachStatusCoach(){
    const status=document.getElementById('labStatus');
    if(!status || status.dataset.v6==='1')return;
    status.dataset.v6='1';
    const box=document.createElement('div');box.id='v6StatusCoach';box.className='v6StatusCoach';status.insertAdjacentElement('afterend',box);
    const update=()=>{const x=explainStatusText(status.textContent);box.innerHTML=x?`<b>💡 ماذا تعني النتيجة؟ — ${esc(x[0])}</b><p>${esc(x[1])}</p>`:'';};
    new MutationObserver(update).observe(status,{childList:true,subtree:true,characterData:true});update();
  }

  function addFieldWhy(){
    const modalEl=document.querySelector('#modalRoot .modal');
    if(!modalEl || modalEl.dataset.v6==='1')return;
    modalEl.dataset.v6='1';
    const help={
      'IP':'هو عنوان الجهاز المنطقي. نحتاجه ليعرف الآخرون أين يرسلون الحزم والردود.',
      'Subnet Mask':'يحدد حدود الشبكة المحلية: هل الهدف محلي أم يجب إرساله إلى Gateway؟',
      'Gateway':'بوابة الخروج إلى الشبكات الأخرى، وعادة هي عنوان واجهة الراوتر داخل نفس LAN.',
      'DNS':'عنوان الخادم الذي يحول أسماء مثل site.local إلى IP.',
      'SSID':'اسم شبكة Wi‑Fi التي يحاول الجهاز الانضمام إليها.',
      'كلمة المرور':'تثبت السماح بالانضمام إلى شبكة Wi‑Fi الصحيحة.',
      'Base':'بداية شبكة العناوين التي سيستخدمها DHCP عند تكوين Pool.',
      'من':'أول رقم Host يسمح DHCP بتوزيعه.',
      'إلى':'آخر رقم Host يسمح DHCP بتوزيعه.'
    };
    modalEl.querySelectorAll('.field label').forEach(l=>{const k=l.textContent.trim();if(help[k])l.insertAdjacentHTML('afterend',`<small class="v6FieldWhy"><b>لماذا؟</b> ${esc(help[k])}</small>`)});
  }

  const root=document.getElementById('screen');
  if(root)new MutationObserver(()=>{enrichGuidedLab();attachStatusCoach();}).observe(root,{childList:true,subtree:true});
  const modalRoot=document.getElementById('modalRoot');
  if(modalRoot)new MutationObserver(addFieldWhy).observe(modalRoot,{childList:true,subtree:true});

  const oldVerify=window.verifyLessonLab;
  if(typeof oldVerify==='function')window.verifyLessonLab=function(i){
    oldVerify(i);
    setTimeout(()=>{
      const box=document.getElementById('guideResult');if(!box||!box.querySelector('.guideFail'))return;
      let issues=[];try{issues=LAB.diagnose()||[]}catch(e){}
      const tips=issues.slice(0,6).map(s=>{const x=explainStatusText(s);return `<li><b>${esc(s)}</b><span>${esc(x?x[1]:'افحص هذا الشرط ثم أعد نفس الاختبار.')}</span></li>`}).join('');
      box.insertAdjacentHTML('beforeend',`<div class="v6FailCoach"><b>🔎 لماذا لم تنجح؟</b>${tips?`<ul>${tips}</ul>`:'<p>لم أجد خطأً عامًا واضحًا، لكن شرط المهمة نفسها غير مكتمل. راجع الأجهزة المطلوبة، التوصيلات، IP/Mask، Gateway، ثم الخدمة التي تختبرها.</p>'}</div>`);
    },50);
  };

  enrichGuidedLab();attachStatusCoach();addFieldWhy();
})();
