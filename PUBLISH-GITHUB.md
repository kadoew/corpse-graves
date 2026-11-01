# نشر المشروع على GitHub وGitHub Pages — خطوة بخطوة

المستودع المحلي جاهز في `D:\mc modes\corpsegraves`، ونُشر على GitHub: `https://github.com/kadoew/corpse-graves`. تبقى الخطوات أدناه مرجعاً للتحديثات والإصدارات القادمة.

## 0) قبل النشر — قرارات مهمة

**هل أنصح بالنشر؟** نعم، لكن بشرطين:

1. **تحقق من الاسم:** ابحث عن `Corpse Graves` و`corpsegraves` على GitHub وModrinth وCurseForge. إن كان الاسم أو معرف المود مستخدماً، غيّره الآن (الاسم + `modid` + مجلدات الموارد) قبل أن يعتمد عليه اللاعبون؛ التغيير لاحقاً صعب.
2. **افهم رخصة MIT:** تسمح لأي شخص بالاستخدام والتعديل والبيع، بشرط إبقاء نص الرخصة وإشعار حقوق النشر. إذا أردت منع البيع التجاري لاحقاً، MIT ليست المناسبة — لكنك اخترتها وهي خيار جيد للانتشار.

تحقق أيضاً من:
- الملفات التي سترفعها لا تحتوي عوالم لاعبين أو سجلات أو توكنات.
- جرة Fabric API تُعاد توزيعها في `docs/downloads`؛ أضف [THIRD-PARTY.md](THIRD-PARTY.md) (موجود) واحتفظ بترخيصها.
- هذا مود غير رسمي وغير تابع لـMojang/Microsoft، ووضّح ذلك في README (موجود).

## 1) إنشاء الحساب والمستودع

1. أنشئ حساباً على https://github.com إن لم يكن لديك.
2. اضغط **New repository**.
3. الاسم: `corpse-graves` (أو اسم متاح).
4. الوصف: `Protected, skin-rendered corpse graves for Fabric, Forge and NeoForge.`
5. اختر **Public** (GitHub Pages المجاني يحتاج مستودعاً عاماً).
6. **لا تفعّل** Add README / .gitignore / license — الملفات موجودة محلياً.
7. اضغط **Create repository**.

## 2) رفع المشروع من جهازك

ثبّت Git for Windows إن لم يكن مثبتاً، ثم من PowerShell:

```powershell
cd "D:\mc modes\corpsegraves"
git config user.name "اسمك"
git config user.email "بريدك@example.com"

git remote add origin https://github.com/USERNAME/corpse-graves.git
git push -u origin main
```

> استبدل `USERNAME` باسم حسابك. المستودع المحلي أُنشئ وفيه commit أول بالفعل؛ لا تحتاج `git init` مرة أخرى. إن أردت التحقق: `git log --oneline`.

بعد الرفع سيظهر المصدر و`LICENSE` والموقع في `docs/` على GitHub.

## 3) تفعيل موقع GitHub Pages

1. في المستودع: **Settings → Pages**.
2. تحت **Build and deployment** اختر **Deploy from a branch**.
3. Branch: `main`.
4. Folder: **`/docs`** (مهم؛ ليس `/root`).
5. اضغط **Save** وانتظر 1–3 دقائق.
6. سيظهر الرابط:
   ```
   https://USERNAME.github.io/corpse-graves/
   ```
7. افتحه وتأكد من عمل:
   - مرشحات Fabric/Forge/NeoForge.
   - تحميل جرة 26.3.
   - تحميل الحزمة الكاملة.
   - دليل التثبيت والبصمات.

الموقع في `docs/` يحتوي `index.html` و`assets/` و`downloads/` وملف `.nojekyll` (يمنع معالجة Jekyll غير الضرورية). الروابط نسبية لذلك تعمل تحت مسار المستودع.

### إن ظهرت 404
- تأكد أن الملف `docs/index.html` مرفوع فعلاً.
- أعد حفظ Pages وانتظر إعادة النشر.
- راجع تبويب **Actions** أو **Pages** لسجل البناء.

### نطاق مخصص (اختياري)
Settings → Pages → Custom domain. أضف CNAME عند مزود النطاق ثم فعّل HTTPS. لا تفعلها قبل التأكد من الموقع الأساسي.

## 4) إنشاء Release للجرار

1. من الصفحة الرئيسية: **Releases → Draft a new release**.
2. Tag: `v1.1.0` — Target: `main`.
3. العنوان: `Corpse Graves 1.1.0`.
4. اكتب ملاحظات الإصدار (الميزات/الإصلاحات).
5. ارفع من `docs/downloads/`:
   - كل جرات المود الـ23 (`corpsegraves-*.jar`).
   - جرات Fabric API السبع.
   - `corpsegraves-all-families.zip` و`SHA256SUMS.txt`.
6. **Publish release**.

GitHub Releases هي المكان المناسب للثنائيات؛ الموقع في `docs/` نسخة مريحة للتحميل المباشر. لا ترفع الجرار إلى فرع المصدر يدوياً مرة أخرى؛ حدّث `docs/downloads` وارفع commit جديداً عند كل إصدار.

## 5) تفعيل Issues والإعدادات

- **Settings → General → Features:** فعّل **Issues** (وDiscussions إن أردت).
- أضف Labels: `bug`, `fabric`, `forge`, `neoforge`, `26.x`, `1.21`, `1.20.1`, `needs-log`.
- قالب البلاغ موجود في `.github/ISSUE_TEMPLATE/bug_report.yml` ويطلب نسخة اللعبة واللودر والسجل.
- فعّل **Private vulnerability reporting** إن ظهر؛ لا تضع بريداً شخصياً إن لم ترغب.

## 6) تحديث README بعد النشر

استبدل `YOUR_USERNAME` في `README.md`:

```powershell
(Get-Content README.md) -replace "YOUR_USERNAME","اسم_حسابك" | Set-Content README.md
git add README.md
git commit -m "docs: set GitHub and Pages links"
git push
```

الموقع نفسه لا يعتمد على اسم المستخدم (روابط نسبية)، لكن وضّح رابط Pages/Releases في README.

## 7) تحديث الموقع مستقبلاً

1. عدّل `corpsegraves-site/dist` (المصدر).
2. انسخه فوق `corpsegraves/docs` (احتفظ بـ`.nojekyll`).
3. حدّث `docs/downloads` والجرار والبصمات والحزمة.
4. `git add docs && git commit -m "site: 1.1.x" && git push`.

Pages يعيد النشر تلقائياً من `main`.

## 8) Modrinth / CurseForge (اختياري)

GitHub مناسب للمصدر والمشاكل والتحميل، لكن اكتشاف المود يكون أفضل على Modrinth/CurseForge. الرفع هناك يحتاج حساب المالك وتوكن؛ لا تضع التوكن في Git. استخدم متغير بيئة محلياً، أو ارفع يدوياً من لوحة الموقع. حزمة الرفع الجاهزة موجودة في `release/` خارج هذا المستودع.

## 9) صيانة سريعة بعد النشر

عند بلاغ عن عطل:

1. افتح Issue واطلب: النسخة، اللودر، الجرة، `latest.log`، وخطوات الإعادة.
2. أصلح في المشروع المرجعي للعائلة (راجع [MAINTAINERS.md](MAINTAINERS.md)).
3. مرّر الإصلاح بـ`sync-shared.ps1` وابنِ الكل بـ`build-all.ps1`.
4. اختبر سيرفراً/عميلاً، وحدّث الإصدار.
5. ارفع Release جديداً وحدّث `docs/` و`README`.

**الوقت المتوقع:** إصلاح منطق مشترك وتمريره وبناء الـ23 واختبار دخان = عادة 1–3 ساعات في جلسة واحدة، وليس نسخة كل سنة. التغييرات الخاصة بـAPI لكل عائلة قد تضيف وقتاً، لكن البناء نفسه آلي.

## 10) ما لا يجب رفعه

`build/`, `run/`, `logs/`, العوالم، ملفات `.env`، التوكنات، مفاتيح Netlify/Telegraph/Modrinth، ومجلدات JDK/Gradle المحلية. ملف `.gitignore` يغطي الأساسيات؛ راجع `git status` قبل كل push.
