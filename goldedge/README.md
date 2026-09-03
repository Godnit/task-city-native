# GoldEdge | مرصد قرار تداول الذهب

تطبيق Android عربي خفيف لتنظيم قرار تداول XAUUSD قبل الدخول. التطبيق لا ينفذ الصفقات ولا يَعِد بنسبة نجاح؛ يجمع سياقًا عمليًا من عدة مصادر ويضيف فلتر SMC، حاسبة مخاطرة، وتنبيهات أخبار وسجل أداء.

## الوظائف
- اتجاه متعدد الأطر M5 / M15 / H1 / H4 / D1 من بيانات Gold Futures المرجعية.
- DXY و US10Y كعوامل سياقية معاكسة للذهب غالبًا.
- تقويم USD من ملف JSON الأسبوعي الذي يتيحه Forex Factory.
- Sentiment لـ XAUUSD من Myfxbook مع توضيح تأخر البيانات المجانية.
- فلتر صفقة SMC يدوي: الاتجاه، السيولة، Sweep، BOS/CHoCH، FVG/OB، الرفض، الجلسة، الأخبار، ومساحة الهدف.
- حاسبة R:R وحجم لوت تقريبي بافتراض Contract Size = 100 oz للوت القياسي.
- سجل صفقات محلي مع Win Rate ومتوسط R ومقارنة الصفقات ذات Setup Score المرتفع.
- تنبيه Android قبل أخبار USD القوية باستخدام WorkManager.
- واجهة عربية RTL ودعم Android 8.0+ (minSdk 26).

## مصادر البيانات
- Forex Factory weekly calendar JSON: `nfs.faireconomy.media/ff_calendar_thisweek.json`
- Myfxbook XAUUSD Outlook
- Yahoo Finance chart endpoint for Gold Futures (`GC=F`), DXY (`DX-Y.NYB`) and US 10Y (`^TNX`)
- TradingView XAUUSD Technicals as an external reference link

> سعر الذهب داخل لوحة التحليل هو **مرجع Futures** وليس سعر وسيط MT5، لذلك يظل سعر الوسيط هو المرجع الفعلي للدخول وSL/TP.

## البناء
GitHub Actions runs unit tests + Android lint + `assembleDebug`, then uploads an installable APK artifact.
